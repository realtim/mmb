build-site:
	docker compose -f docker-compose-site.yml build

up-site: \
	stop-site \
	docker-up-site

stop-site:
	docker compose -f docker-compose-site.yml stop

docker-up-site:
	docker compose -f docker-compose-site.yml up -d --build --remove-orphans

down-site:
	docker compose down -f docker-compose-site.yml --volumes --remove-orphans --rmi local

create-setting-site:
	docker compose -f docker-compose-site.yml cp -a environment/dev/php/settings.php php:/var/www/html/settings.php
	docker compose -f docker-compose-site.yml run --rm php mkdir logs

mysql-import-site:
	docker compose -f docker-compose-site.yml exec mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS mmb;"
	docker compose -f docker-compose-site.yml exec mysql mysql -uroot -proot -e "CREATE DATABASE mmb;"
	docker compose -f docker-compose-site.yml exec -e MYSQL_PWD=mmb mysql sh -c 'exec pv /sql/mmb_structure.sql | mysql -u mmb mmb'
	docker compose -f docker-compose-site.yml exec -e MYSQL_PWD=mmb mysql sh -c 'exec pv /tmp/Fixture.sql | mysql -u mmb mmb'

init-site: \
	build-site \
	docker-up-site \
	create-setting-site \
	mysql-import-site