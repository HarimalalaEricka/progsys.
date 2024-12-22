create database ftp;
use ftp;
create table ftp ( 
    nomFichier varchar(200)
);

insert into ftp(nomFichier) values("q.pdf");

drop table ftp;