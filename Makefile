build:
	cd dropwizard-0.7-opentracing && mvn -s ../settings.xml install
	cd dropwizard-opentracing && mvn -s ../settings.xml install

publish: check-env-vars
	cd dropwizard-0.7-opentracing && mvn -s ../settings.xml clean deploy
	cd ../dropwizard-opentracing && mvn -s ../settings.xml clean deploy

check-env-vars:
	if [ -z "${OSSRH_USERNAME}" -o -z "${OSSRH_PASSWORD}" -o -z "${GPG_PASSWORD}" ] ; then echo "\n\nERROR: Missing required environment variables; see the Makefile\n\n" ; exit 1 ; fi
