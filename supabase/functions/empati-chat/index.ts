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

/**
 * Menyusun jawaban berbahasa Indonesia yang hangat dan berlandaskan blok [KONTEKS SKRINING]
 * serta [RUJUKAN] pada userPrompt ketika upstream LLM publik sedang penuh/timeout.
 */
function synthesizeGroundedFallback(userPrompt: string): string {
  const queryMatch = userPrompt.match(/Pertanyaan pengguna:\s*(.+)/i);
  const rawQuery = (queryMatch ? queryMatch[1].trim() : userPrompt.trim()).toLowerCase();

  const screeningSectionMatch = userPrompt.match(/\[KONTEKS SKRINING\]([\s\S]*?)(\[RUJUKAN\]|$)/i);
  const screeningSection = screeningSectionMatch ? screeningSectionMatch[1].trim() : "";

  const rujukanSectionMatch = userPrompt.match(/\[RUJUKAN\]([\s\S]*)$/i);
  const rujukanSection = rujukanSectionMatch ? rujukanSectionMatch[1].trim() : "";

  const parsedChunks: Array<{ publisher: string; text: string }> = [];
  if (rujukanSection && !rujukanSection.includes("Tidak ada rujukan yang cocok")) {
    const lines = rujukanSection.split("\n");
    for (const line of lines) {
      const m = line.match(/^\d+\.\s*\([^)]+\)\s*[^,]+,\s*([^,]+),\s*tahun\s*\d+,\s*bagian\s*[^:]+:\s*(.+)$/i);
      if (m) {
        parsedChunks.push({
          publisher: m[1].trim(),
          text: m[2].trim()
        });
      }
    }
  }

  const hasScreening =
    screeningSection.length > 0 &&
    !screeningSection.includes("Belum ada hasil skrining");

  const asksAboutScreening =
    rawQuery.includes("hasil") ||
    rawQuery.includes("skrining") ||
    rawQuery.includes("screening") ||
    rawQuery.includes("kondisi saya") ||
    rawQuery.includes("risiko saya") ||
    rawQuery.includes("simpul") ||
    rawQuery.includes("analisis");

  const paragraphs: string[] = [];

  if (hasScreening && asksAboutScreening) {
    const cleanScreening = screeningSection
      .split("\n")
      .map((l) => l.replace(/^-+\s*/, "").trim())
      .filter((l) => l.length > 0)
      .join(". ");
    paragraphs.push(
      `Berdasarkan konteks skrining Anda saat ini, ${cleanScreening}`
    );
  }

  if (parsedChunks.length > 0) {
    const primary = parsedChunks[0];
    const secondary = parsedChunks.length > 1 ? parsedChunks[1] : null;
    if (secondary && secondary.publisher !== primary.publisher) {
      paragraphs.push(
        `Mengacu pada rujukan ${primary.publisher}, ${primary.text} Selain itu, panduan dari ${secondary.publisher} juga menjelaskan bahwa ${secondary.text.charAt(0).toLowerCase()}${secondary.text.slice(1)}`
      );
    } else if (secondary) {
      paragraphs.push(
        `Mengacu pada panduan ${primary.publisher}, ${primary.text} ${secondary.text}`
      );
    } else {
      paragraphs.push(
        `Mengacu pada panduan ${primary.publisher}, ${primary.text}`
      );
    }
  } else if (!hasScreening || !asksAboutScreening) {
    return "Saat ini saya belum memiliki rujukan yang cukup untuk menjawab pertanyaan tersebut secara pasti. Anda dapat menanyakan hal seputar tekanan darah, pola makan rendah garam, aktivitas fisik, kualitas tidur, pengelolaan stres, atau melakukan skrining di aplikasi Empati serta berkonsultasi dengan tenaga kesehatan.";
  }

  if (
    rawQuery.includes("kekerasan") ||
    rawQuery.includes("kdrt") ||
    rawQuery.includes("kbg") ||
    rawQuery.includes("pelecehan") ||
    rawQuery.includes("ancaman") ||
    rawQuery.includes("sapa") ||
    screeningSection.toLowerCase().includes("kekerasan")
  ) {
    paragraphs.push(
      "Bila Anda atau seseorang di dekat Anda membutuhkan ruang cerita dan pendampingan yang aman, Layanan SAPA 129 dari Kementerian Pemberdayaan Perempuan dan Perlindungan Anak dapat dihubungi melalui telepon 129 atau WhatsApp 08111-129-129."
    );
  } else {
    paragraphs.push(
      "Langkah kecil yang konsisten seperti memantau tekanan darah secara berkala, membatasi asupan garam harian, menjaga kecukupan tidur malam, serta berkonsultasi ke tenaga kesehatan bila ada keluhan akan sangat membantu menjaga kesehatan Anda."
    );
  }

  return paragraphs.join("\n\n");
}

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
              "HTTP-Referer": "https://mevrnxtrulidgpgfipsc.supabase.co",
              "X-Title": "Empati Health & Psychosocial Companion"
            },
            body: JSON.stringify({
              model,
              messages,
              temperature: 0.3,
              top_p: 0.85,
              max_tokens: 512
            }),
            signal: AbortSignal.timeout(8000)
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

    // Fallback ke endpoint OpenAI-compatible dengan batas waktu 5 detik agar tidak menggantung
    try {
      const fallbackResp = await fetch(FALLBACK_OPENAI_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "User-Agent": "Empati/4.1.5"
        },
        body: JSON.stringify({
          model: "openai",
          messages
        }),
        signal: AbortSignal.timeout(5000)
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
    } catch (_fallbackErr) {
      // Lanjut ke penyusun jawaban tergrounded berbasis rujukan korpus
    }

    const synthesized = synthesizeGroundedFallback(userPrompt);
    return new Response(
      JSON.stringify({
        provider: "empati-edge-grounded",
        model: "empati-cloud-grounded",
        message: { role: "assistant", content: synthesized }
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      }
    );
  } catch (err) {
    return new Response(JSON.stringify({ error: String(err) }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }
});
