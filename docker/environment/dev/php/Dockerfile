FROM php:7.2.29-apache

RUN apt-get update -y && apt-get install -y libpng-dev golang-go git

RUN mkdir -p /root/gocode \
  && export GOPATH=/root/gocode \
  && go get github.com/mailhog/mhsendmail \
  && mv /root/gocode/bin/mhsendmail /usr/local/bin \
  && rm -rf /root/gocode

RUN docker-php-ext-install mysqli

RUN docker-php-ext-install gd

RUN docker-php-ext-install zip

COPY ./conf.d /usr/local/etc/php/conf.d

RUN usermod -u 1000 www-data && groupmod -g 1000 www-data

USER www-data