insert into category (slug, name) values
    ('audio', 'Audio'),
    ('desk', 'Desk'),
    ('keyboards', 'Keyboards');

insert into product (sku, name, description, price_cents, stock_qty, image_url, category_id)
select 'AUD-1001', 'Studio Monitor Headphones',
       'Closed-back monitors with a flat response, made for long mixing sessions rather than commuting.',
       18900, 12, '/img/aud-1001.jpg', id from category where slug = 'audio';

insert into product (sku, name, description, price_cents, stock_qty, image_url, category_id)
select 'AUD-1002', 'USB Audio Interface',
       'Two inputs, phantom power, and latency low enough to track vocals against a busy session.',
       12500, 3, '/img/aud-1002.jpg', id from category where slug = 'audio';

insert into product (sku, name, description, price_cents, stock_qty, image_url, category_id)
select 'DSK-2001', 'Standing Desk Frame',
       'Dual-motor frame rated to 120kg with a memory controller and a genuinely quiet lift.',
       42000, 6, '/img/dsk-2001.jpg', id from category where slug = 'desk';

insert into product (sku, name, description, price_cents, stock_qty, image_url, category_id)
select 'DSK-2002', 'Monitor Arm',
       'Gas-spring arm with a clamp mount, holds a 32 inch panel without drooping over the week.',
       8900, 0, '/img/dsk-2002.jpg', id from category where slug = 'desk';

insert into product (sku, name, description, price_cents, stock_qty, image_url, category_id)
select 'KEY-3001', '65 Percent Mechanical Keyboard',
       'Hot-swap board with a gasket mount, tuned so it sounds like a keyboard and not a biscuit tin.',
       14900, 20, '/img/key-3001.jpg', id from category where slug = 'keyboards';

insert into product (sku, name, description, price_cents, stock_qty, image_url, category_id)
select 'KEY-3002', 'Low Profile Wireless Keyboard',
       'Slim scissor switches, three paired devices, and a battery that survives a long trip.',
       9900, 9, '/img/key-3002.jpg', id from category where slug = 'keyboards';
