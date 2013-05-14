APP_VERSION:=$(shell grep "public static final String appVersion" src/DatatoolTk.java | sed "s/public\sstatic\sfinal\sString\sappVersion=//" | tr -d "\"\; ")

test	: app
	bin/datatooltk

app	: lib/datatooltk.jar lib/jh.jar lib/jlfgr-1_0.jar

lib/datatooltk.jar	: src/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/DatatoolTk.class
	cd classes; \
	jar cmf ../src/Manifest.txt ../lib/datatooltk.jar \
	com/dickimawbooks/datatooltk/*.class

classes/com/dickimawbooks/datatooltk/DatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	src/*.java lib/jh.jar lib/jlfgr-1_0.jar
	cd src; \
	javac -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/jh.jar:../lib/jlfgr-1_0.jar *.java

classes/com/dickimawbooks/datatooltk	:
	mkdir -p classes/com/dickimawbooks/datatooltk

clean	:
	\rm -f classes/com/dickimawbooks/datatooltk/*.class

squeaky	:
	\rm -f lib/datatooltk.jar
