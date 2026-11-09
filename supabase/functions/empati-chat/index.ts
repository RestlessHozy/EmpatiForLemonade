import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const FALLBACK_OPENAI_URL = "https://text.pollinations.ai/openai";

/**
 * Daftar prioritas model OpenRouter untuk akun GRATIS (Free Tier - 0 kredit).
 * Seluruh model berakhiran `:free` dan router `openrouter/free` tidak memotong saldo ($0).
 */
const FREE_OPENROUTER_MODELS = [
  "openrouter/free",
  "meta-llama/llama-3.3-70b-instruct:free",
  "google/gemma-3-12b-it:free",
  "qwen/qwen3-8b:free"
];

/**
 * Model berbayar (opsional, hanya dicoba bila akun memiliki saldo kredit dan seluruh model gratis sedang penuh).
 */
const PAID_FALLBACK_MODELS = [
  "qwen/qwen-2.5-7b-instruct",
  "openai/gpt-4o-mini"
];

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-openrouter-key",
  "Access-Control-Allow-Methods": "POST, OPTIONS"
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }

  try {
    const body = await req.json();
    const systemPrompt: string = body.systemPrompt || "";
    const userPrompt: string = body.userPrompt || "";
    const requestedModel: string = body.model || FREE_OPENROUTER_MODELS[0];

    if (!userPrompt.trim()) {
      return new Response(JSON.stringify({ error: "Empty userPrompt" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    const headerOpenRouterKey = req.headers.get("x-openrouter-key")?.trim() || "";
    const envOpenRouterKey = Deno.env.get("OPENROUTER_API_KEY")?.trim() || "";
    const openRouterKey = headerOpenRouterKey || envOpenRouterKey;

    const messages = [
      ...(systemPrompt ? [{ role: "system", content: systemPrompt }] : []),
      { role: "user", content: userPrompt }
    ];

    if (openRouterKey) {
      // Utamakan seluruh model GRATIS (:free & openrouter/free) terlebih dahulu agar akun gratis langsung berhasil
      const candidateModels = requestedModel.endsWith(":free") || requestedModel === "openrouter/free"
        ? [
            requestedModel,
            ...FREE_OPENROUTER_MODELS.filter((m) => m !== requestedModel),
            ...PAID_FALLBACK_MODELS
          ]
        : [
            ...FREE_OPENROUTER_MODELS,
            requestedModel,
            ...PAID_FALLBACK_MODELS.filter((m) => m !== requestedModel)
          ];

      for (const model of candidateModels) {
        try {
          const resp = await fetch(OPENROUTER_URL, {
            method: "POST",
            headers: {
              "Authorization": `Bearer ${openRouterKey}`,
              "Content-Type": "application/json",
              "HTTP-Referer": "https://qnvcuhmvrrffzdxxdloc.supabase.co",
              "X-Title": "SRYA Emora Health & Psychosocial Companion"
            },
            body: JSON.stringify({
              model,
              messages,
              temperature: 0.3,
              top_p: 0.85,
              max_tokens: 512
            })
          });

          if (resp.ok) {
            const data = await resp.json();
            const content = data?.choices?.[0]?.message?.content?.trim();
            if (content) {
              return new Response(
                JSON.stringify({
                  provider: "openrouter",
                  model: data?.model || model,
                  message: { role: "assistant", content }
                }),
                {
                  status: 200,
                  headers: { ...corsHeaders, "Content-Type": "application/json" }
                }
              );
            }
          }
        } catch (_err) {
          // Lanjut ke kandidat model OpenRouter berikutnya
        }
      }
    }

    // Fallback ke endpoint OpenAI-compatible jika semua model OpenRouter sedang rate-limit
    const fallbackResp = await fetch(FALLBACK_OPENAI_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": "SRYA-Emora/4.1.3"
      },
      body: JSON.stringify({
        model: "openai",
        messages
      })
    });

    if (fallbackResp.ok) {
      const fallbackData = await fallbackResp.json();
      const content =
        fallbackData?.choices?.[0]?.message?.content?.trim() ||
        fallbackData?.message?.content?.trim() ||
        "";
      if (content) {
        return new Response(
          JSON.stringify({
            provider: "openrouter-edge-fallback",
            model: "openrouter-auto",
            message: { role: "assistant", content }
          }),
          {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" }
          }
        );
      }
    }

    return new Response(JSON.stringify({ error: "Upstream LLM unavailable" }), {
      status: 502,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: String(err) }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }
});
