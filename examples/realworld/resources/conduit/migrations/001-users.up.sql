CREATE TABLE users (
 id SERIAL PRIMARY KEY,
 email TEXT UNIQUE,
 username TEXT UNIQUE,
 password TEXT,
 bio TEXT,
 image TEXT
)
