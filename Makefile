build-site:
	docker compose build

up-site: \
	stop-site \
	docker-up-site

stop-site:
	docker compose stop

docker-up-site:
	docker compose up -d --build --remove-orphans

down-site:
	docker compose down --volumes --remove-orphans --rmi local

create-setting-site:
	docker compose cp -a environment/dev/php/settings.php php:/var/www/html/settings.php
	docker compose run --rm php mkdir logs

mysql-import-site:
	docker compose exec mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS mmb;"
	docker compose exec mysql mysql -uroot -proot -e "CREATE DATABASE mmb;"
	docker compose exec -e MYSQL_PWD=mmb mysql sh -c 'exec pv /sql/mmb_structure.sql | mysql -u mmb mmb'
	docker compose exec -e MYSQL_PWD=mmb mysql sh -c 'exec pv /sql/Fixture.sql | mysql -u mmb mmb'

init-site: \
	build-site \
	docker-up-site \
	create-setting-site \
	mysql-import-site \