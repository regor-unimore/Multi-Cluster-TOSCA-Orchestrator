#!/bin/bash
echo "BPMN_ENGINE is set to" $BPMN_ENGINE
echo "SERVICE_BROKER_URI is set to" $SERVICE_BROKER_URI
echo "APP_URL is set to" $APP_URL

sed -E -i_backup -e "s|^BPMN_ENGINE=.*|BPMN_ENGINE=$BPMN_ENGINE|;s|^SERVICE_BROKER_URI=.*|SERVICE_BROKER_URI=$SERVICE_BROKER_URI|;s|^APP_URL=.*|APP_URL=$APP_URL|"   .env

composer install
npm install
php artisan key:generate
php artisan migrate:install
php artisan migrate
php artisan storage:link
npm run development

CURRENT_PWD=$(pwd)
cd ./public/json4tosca-parser/
pip3 install --requirement requirements.txt
PBR_VERSION=1.2.3 python3 setup.py sdist

cd $CURRENT_PWD
php artisan serve --host=0.0.0.0

sleep infinity
