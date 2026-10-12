INSERT INTO categories (id, name, description) VALUES
 ('10000000-0000-0000-0000-000000000001', 'Electronics',    'Gadgets and accessories'),
 ('10000000-0000-0000-0000-000000000002', 'Books',          'Technical and general reading'),
 ('10000000-0000-0000-0000-000000000003', 'Clothing',       'Everyday and outdoor wear'),
 ('10000000-0000-0000-0000-000000000004', 'Home & Kitchen', 'Make your home nicer');

INSERT INTO products (id, name, description, sku, price, image_url, category_id) VALUES
 ('20000000-0000-0000-0000-000000000001', 'Wireless Noise-Cancelling Headphones', 'Over-ear Bluetooth headphones with 30-hour battery life.', 'ELEC-1001', 129.99, 'https://picsum.photos/seed/ELEC-1001/600/400', '10000000-0000-0000-0000-000000000001'),
 ('20000000-0000-0000-0000-000000000002', 'Mechanical Keyboard', 'Hot-swappable 75% keyboard with tactile switches.', 'ELEC-1002', 89.50, 'https://picsum.photos/seed/ELEC-1002/600/400', '10000000-0000-0000-0000-000000000001'),
 ('20000000-0000-0000-0000-000000000003', '4K Action Camera', 'Waterproof camera with electronic image stabilisation.', 'ELEC-1003', 199.00, 'https://picsum.photos/seed/ELEC-1003/600/400', '10000000-0000-0000-0000-000000000001'),
 ('20000000-0000-0000-0000-000000000004', 'Clean Code', 'A handbook of agile software craftsmanship.', 'BOOK-2001', 34.99, 'https://picsum.photos/seed/BOOK-2001/600/400', '10000000-0000-0000-0000-000000000002'),
 ('20000000-0000-0000-0000-000000000005', 'Designing Data-Intensive Applications', 'The big ideas behind reliable, scalable systems.', 'BOOK-2002', 49.90, 'https://picsum.photos/seed/BOOK-2002/600/400', '10000000-0000-0000-0000-000000000002'),
 ('20000000-0000-0000-0000-000000000006', 'Cotton Crew-Neck T-Shirt', 'Soft 100% organic cotton tee.', 'CLTH-3001', 15.99, 'https://picsum.photos/seed/CLTH-3001/600/400', '10000000-0000-0000-0000-000000000003'),
 ('20000000-0000-0000-0000-000000000007', 'Trail Running Jacket', 'Lightweight, packable and water resistant.', 'CLTH-3002', 74.00, 'https://picsum.photos/seed/CLTH-3002/600/400', '10000000-0000-0000-0000-000000000003'),
 ('20000000-0000-0000-0000-000000000008', 'Wool Beanie', 'Merino wool beanie for cold mornings.', 'CLTH-3003', 18.00, 'https://picsum.photos/seed/CLTH-3003/600/400', '10000000-0000-0000-0000-000000000003'),
 ('20000000-0000-0000-0000-000000000009', 'Stainless Steel French Press', '1-litre double-wall French press.', 'HOME-4001', 29.95, 'https://picsum.photos/seed/HOME-4001/600/400', '10000000-0000-0000-0000-000000000004'),
 ('20000000-0000-0000-0000-000000000010', 'Ceramic Pour-Over Set', 'Dripper and server for slow coffee.', 'HOME-4002', 24.50, 'https://picsum.photos/seed/HOME-4002/600/400', '10000000-0000-0000-0000-000000000004');
