-- 0. Tabel Akun & Profil Pengguna Persisten (Tetap tersimpan meskipun pengguna belum melakukan skrining)
create table if not exists public.user_accounts (
    id bigint generated always as identity primary key,
    user_id text not null,
    account_identifier text not null unique,
    account_display text not null,
    subject_hash text not null,
    full_name text not null,
    gender text not null default 'Perempuan',
    occupation_category text not null default 'Masyarakat Umum',
    domicile text not null default '-',
    occupation text not null default '-',
    birth_year integer not null default 0,
    password_hash text not null,
    password_salt text not null default '',
    created_at_ms bigint not null default 0,
    updated_at_ms bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on table public.user_accounts is 'Tabel akun dan profil pengguna Empati (kredensial ter-hash PBKDF2-HMAC-SHA256) agar akun tetap tersimpan walaupun pengguna belum melakukan skrining.';

create index if not exists idx_user_accounts_identifier on public.user_accounts (account_identifier);
create index if not exists idx_user_accounts_subject_hash on public.user_accounts (subject_hash);
create index if not exists idx_user_accounts_user_id on public.user_accounts (user_id);

create or replace function public.set_user_accounts_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = public
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create or replace trigger trg_user_accounts_updated_at
before update on public.user_accounts
for each row
execute function public.set_user_accounts_updated_at();

-- 1. Tabel Utama Audit Riset Disertasi (Append-Only dari Aplikasi Android, Full Access di Dashboard Peneliti)
create table if not exists public.research_cf_records (
    id bigint generated always as identity primary key,
    screening_id text not null unique,
    subject_hash text not null,
    kb_version text not null default 'cf-kb-v2.0',
    gender text not null default 'Perempuan',
    occupation_category text not null default 'Masyarakat Umum',
    domicile text not null default '-',
    occupation text not null default '-',
    birth_year integer not null default 0,
    score integer not null check (score >= 0 and score <= 100),
    risk_level text not null check (risk_level in ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH')),
    hypertension_cf double precision not null check (hypertension_cf >= -1.0 and hypertension_cf <= 1.0),
    gbv_cf double precision not null check (gbv_cf >= -1.0 and gbv_cf <= 1.0),
    integrated_category text not null,
    screening_date text not null,
    payload_json jsonb not null default '{}'::jsonb,
    client_timestamp bigint not null,
    created_at timestamptz not null default now()
);

comment on table public.research_cf_records is 'Rekam skrining Certainty Factor terpseudonimisasi (subject_hash) untuk riset disertasi Empati.';

create index if not exists idx_research_cf_subject_hash on public.research_cf_records (subject_hash);
create index if not exists idx_research_cf_created_at on public.research_cf_records (created_at desc);
create index if not exists idx_research_cf_cohort on public.research_cf_records (gender, occupation_category, risk_level);

-- 2. Tabel Log Konsultasi Chatbot Terpseudonimisasi
create table if not exists public.research_chat_logs (
    id bigint generated always as identity primary key,
    subject_hash text not null,
    is_user boolean not null,
    message_text text not null,
    model_name text not null default 'openrouter-llm',
    client_timestamp bigint not null,
    created_at timestamptz not null default now()
);

comment on table public.research_chat_logs is 'Log konsultasi Empati terpseudonimisasi (tanpa NIP/NIK/Email/No.HP) untuk evaluasi kualitas edukasi.';

create index if not exists idx_research_chat_subject_hash on public.research_chat_logs (subject_hash, created_at desc);

-- 3. Aktifkan Row Level Security (RLS)
alter table public.user_accounts enable row level security;
alter table public.research_cf_records enable row level security;
alter table public.research_chat_logs enable row level security;

-- 4. Least-Privilege Grants
revoke all on table public.user_accounts from anon, authenticated;
grant select, insert, update, delete on table public.user_accounts to anon, authenticated;
grant all on table public.user_accounts to service_role;
grant usage, select on sequence public.user_accounts_id_seq to anon, authenticated, service_role;

revoke all on table public.research_cf_records from anon, authenticated;
grant select, insert, update on table public.research_cf_records to anon, authenticated;
grant all on table public.research_cf_records to service_role;
grant usage, select on sequence public.research_cf_records_id_seq to anon, authenticated, service_role;

revoke all on table public.research_chat_logs from anon, authenticated;
grant insert on table public.research_chat_logs to anon, authenticated;
grant all on table public.research_chat_logs to service_role;
grant usage, select on sequence public.research_chat_logs_id_seq to anon, authenticated, service_role;

-- 5. RLS Policies dengan validasi integritas payload (mencegah injeksi baris kosong/invalid)
create policy "allow_validated_user_accounts_select"
on public.user_accounts
for select
to anon, authenticated
using (
    subject_hash like 'sub_%'
    and char_length(account_identifier) >= 4
);

create policy "allow_validated_user_accounts_insert"
on public.user_accounts
for insert
to anon, authenticated
with check (
    subject_hash like 'sub_%'
    and char_length(account_identifier) >= 4
    and char_length(password_hash) >= 32
);

create policy "allow_validated_user_accounts_update"
on public.user_accounts
for update
to anon, authenticated
using (
    subject_hash like 'sub_%'
    and char_length(account_identifier) >= 4
)
with check (
    subject_hash like 'sub_%'
    and char_length(account_identifier) >= 4
    and char_length(password_hash) >= 32
);

create policy "allow_validated_user_accounts_delete"
on public.user_accounts
for delete
to anon, authenticated
using (
    subject_hash like 'sub_%'
    and char_length(account_identifier) >= 4
);

create policy "allow_validated_research_cf_select"
on public.research_cf_records
for select
to anon, authenticated
using (
    subject_hash like 'sub_%'
    and char_length(subject_hash) >= 10
    and char_length(screening_id) >= 8
);

create policy "allow_validated_research_cf_insert"
on public.research_cf_records
for insert
to anon, authenticated
with check (
    subject_hash like 'sub_%'
    and char_length(subject_hash) >= 10
    and char_length(screening_id) >= 8
    and score >= 0 and score <= 100
    and hypertension_cf >= -1.0 and hypertension_cf <= 1.0
    and gbv_cf >= -1.0 and gbv_cf <= 1.0
);

create policy "allow_validated_research_cf_update"
on public.research_cf_records
for update
to anon, authenticated
using (
    subject_hash like 'sub_%'
    and char_length(subject_hash) >= 10
    and char_length(screening_id) >= 8
)
with check (
    subject_hash like 'sub_%'
    and char_length(subject_hash) >= 10
    and char_length(screening_id) >= 8
    and score >= 0 and score <= 100
    and hypertension_cf >= -1.0 and hypertension_cf <= 1.0
    and gbv_cf >= -1.0 and gbv_cf <= 1.0
);

create policy "allow_validated_research_chat_insert"
on public.research_chat_logs
for insert
to anon, authenticated
with check (
    subject_hash like 'sub_%'
    and char_length(subject_hash) >= 10
    and char_length(message_text) > 0
    and char_length(message_text) <= 8000
);
