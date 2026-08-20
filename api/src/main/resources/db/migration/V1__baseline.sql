create table category (
    id   bigserial primary key,
    slug varchar(80)  not null unique,
    name varchar(120) not null
);

create table product (
    id           bigserial primary key,
    sku          varchar(60)  not null unique,
    name         varchar(200) not null,
    description  text,
    price_cents  bigint       not null check (price_cents >= 0),
    currency     varchar(3)   not null default 'USD',
    stock_qty    integer      not null default 0 check (stock_qty >= 0),
    image_url    varchar(500),
    active       boolean      not null default true,
    category_id  bigint       not null references category (id),
    version      bigint       not null default 0,
    created_at   timestamptz  not null default now()
);

-- Browse always filters on active, usually on category, and orders by
-- created_at. This index covers that path instead of leaving it a seq scan.
create index idx_product_active_category_created
    on product (active, category_id, created_at desc);

create table cart (
    id           bigserial primary key,
    public_token varchar(64) not null unique,
    created_at   timestamptz not null default now()
);

create table cart_item (
    id               bigserial primary key,
    cart_id          bigint  not null references cart (id) on delete cascade,
    product_id       bigint  not null references product (id),
    quantity         integer not null check (quantity > 0),
    unit_price_cents bigint  not null check (unit_price_cents >= 0),
    constraint uq_cart_item_cart_product unique (cart_id, product_id)
);

create index idx_cart_item_cart on cart_item (cart_id);
