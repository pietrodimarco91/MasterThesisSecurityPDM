-- Copyright (C) 2001 YesSoftware. All rights reserved.
-- BugTrack_MySQL.sql

DROP TABLE IF EXISTS simple_test;

CREATE TABLE simple_test (
       card_type_id         int auto_increment primary key,
      firstname             varchar(50) NOT NULL,
  lastname             varchar(50) NOT NULL,
  A             int default 0,
  B             int default 0,
  C             int default 0,
  D             int default 0,
  E             int default 0,
  F             int default 0,
  G             int default 0

);

insert into simple_test (firstname, lastname, A, B, C, D, E, F,G) values(2, 'Web Database Development : Step by Step', 'Jim Buyens', 39.99, 'http://www.amazon.com/exec/obidos/ASIN/0735609667/yessoftware', 'images/books/0735609667.jpg', 'As Web sites continue to grow in complexity and in the volume of data they must present, databases increasingly drive their content. WEB DATABASE DEVELOPMENT FUNDAMENTALS is ideal for the beginning-to-intermediate Web developer, departmental power user, or entrepreneur who wants to step up to a database-driven Web site-without buying several in-depth guides to the different technologies involved. This book uses the clear Microsoft(r) Step by Step tutorial method to familiarize developers with the technologies for building smart Web sites that present data more easily. ', 1);

