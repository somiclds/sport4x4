# Users schema

# --- !Ups

CREATE TABLE account (
    id int(11) NOT NULL AUTO_INCREMENT,
    email varchar(255) NOT NULL,
    password varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    role varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE app (
    id int(11) NOT NULL AUTO_INCREMENT,
    auto_make varchar(255) NOT NULL,
    license_plate varchar(255) NOT NULL,
    driver varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    phone_numb varchar(255) NOT NULL,
    address varchar(255) NOT NULL,
    member_1 varchar(255) NOT NULL,
    member_2 varchar(255) NOT NULL,
    member_3 varchar(255) NOT NULL,
    member_4 varchar(255) NOT NULL,
    sent DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE post (
  id int(11) NOT NULL AUTO_INCREMENT,
  title varchar(255) NOT NULL,
  body TEXT NOT NULL,
  author_id int(11),
  editor_id int(11),
  created DATETIME,
  modified DATETIME,
  post_category int(11),
  post_type int(11),
  PRIMARY KEY (id)
);

# --- !Downs

#DROP TABLE account;

#DROP TABLE app;

#DROP TABLE post;