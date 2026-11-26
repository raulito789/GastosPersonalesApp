-- Esquema SQL para Supabase (PostgreSQL + Row Level Security)
-- Ejecuta este script en el SQL Editor de tu proyecto Supabase para sincronización en vivo.

CREATE TABLE IF NOT EXISTS public.categorias (
    id BIGINT PRIMARY KEY,
    nombre TEXT NOT NULL,
    icono TEXT NOT NULL,
    color_hex BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS public.presupuestos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    categoria_id BIGINT NOT NULL REFERENCES public.categorias(id) ON DELETE CASCADE,
    monto_limite DOUBLE PRECISION NOT NULL CHECK (monto_limite > 0),
    periodo TEXT NOT NULL CHECK (periodo IN ('SEMANAL', 'MENSUAL')),
    alerta_porcentaje INT NOT NULL DEFAULT 80,
    UNIQUE (user_id, categoria_id)
);

CREATE TABLE IF NOT EXISTS public.gastos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    local_id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    titulo TEXT NOT NULL,
    monto DOUBLE PRECISION NOT NULL CHECK (monto > 0),
    fecha_epoch_millis BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL REFERENCES public.categorias(id) ON DELETE RESTRICT,
    updated_at BIGINT NOT NULL,
    UNIQUE (user_id, local_id)
);

ALTER TABLE public.presupuestos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.gastos ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Usuarios gestionan sus presupuestos"
    ON public.presupuestos
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Usuarios gestionan sus gastos"
    ON public.gastos
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

INSERT INTO public.categorias (id, nombre, icono, color_hex) VALUES
    (1, 'Alimentación', 'restaurant', 4279203438),
    (2, 'Transporte', 'directions_car', 4280640491),
    (3, 'Hogar', 'home', 4292441862),
    (4, 'Salud', 'medical_services', 4292617766),
    (5, 'Entretenimiento', 'movie', 4286331885),
    (6, 'Educación', 'school', 4278556265)
ON CONFLICT (id) DO NOTHING;
