set LIBS=lib/commons-cli-1.2.jar;lib/commons-logging-1.1.1.jar;lib/httpclient-4.2.5.jar;lib/httpcore-4.2.4.jar;lib/jsoup-1.6.3.jar

java -cp bin;%LIBS% howdoi.Main %*
