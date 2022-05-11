HadoopHdfsTest
=============================================

### ДАННЫй РЕПОЗИТОРИЙ СОДЕРЖИТ ВЫПОЛНЕННОЕ ТЕСТОВОЕ ЗАДАНИЕ НА ПОЗИЦИЮ QA-ИНЖЕНЕР:
- Поднять кластер Hadoop
- Составить короткий тест-план сервиса hdfs
- Написать автотесты любых 3-ёх кейсов из тест-плана
Ипсользовать паттерны разработки, можно использовать docker image (https://hub.docker.com/r/cloudera/auickstart/, https://hub.docker.com/r/gvacaliuc/hadoop-docker/). 

СОДЕРЖАНИЕ:
- [A. УСТАНОВКА И НАСТРОЙКА КЛАСТЕРА HADOOP С ПОМОЩЬЮ DOCKER](#111)
- [B. ТЕСТ-ПЛАН СЕРВИСА HDFS](#222)
- [С. АВТОТЕСТЫ](#333)

_____


<a name="111"><a>
A. УСТАНОВКА И НАСТРОЙКА КЛАСТЕРА HADOOP С ПОМОЩЬЮ DOCKER
---------------------------------------------

### I. Установка Docker на OS Linux

:white_check_mark: Выполнить обновление системы, установить пакет Docker

```php
$ sudo apt update
$ sudo apt install apt-transport-https ca-certificates curl gnupg-agent software-properties-common
$ sudo rm /etc/apt/preferences.d/nosnap.pref
$ sudo apt update
$ sudo apt install snapd
$ sudo snap install docker
```
:white_check_mark: Проверить установку
```php
$ sudo docker -v
   Docker version 20.10.12, build e91ed5707e
$ sudo docker-compose -v
   docker-compose version 1.29.2, build unknown
$ sudo docker-machine --version
   docker-machine version 0.16.2, build bd45ab1
```
   
:white_check_mark: Добавить репозиторий Docker в Linux
```php
$ sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(. /etc/os-release; echo "$UBUNTU_CODENAME") stable"
```

:white_check_mark: Установить дополнительно docker-ce (и с ним все пакеты, которые требуются для работы Docker, такие как
 containerd.io docker-ce-cli docker-ce-rootless-extras docker-scan-plugin git git-man liberror-perl pigz slirp4netns ...)
 ```php
$ sudo apt update
$ sudo apt install docker-ce 
``` 

:white_check_mark: После установки будет создана группа докеров, добавить своего пользователя в группу, которая будет запускать команды докеров
 ```php
$ sudo usermod -aG docker $USER
```

:white_check_mark: Пока нет ни одного образа
 ```php
larisa@larisa:/home$ sudo docker images
  REPOSITORY   TAG       IMAGE ID   CREATED   SIZE
```

### II. Создание docker-образа на базе Hadoop

:white_check_mark: Скачать docker-образ на базе hadoop (c репозитория docker hub), например
```php
A. https://hub.docker.com/r/cloudera/quickstart/   
   Single-node deployment of Cloudera's 100% open-source Hadoop platform, and Cloudera Manager 

B. https://hub.docker.com/r/gvacaliuc/hadoop-docker/
   Local psuedo-distributed Hadoop 2.7.4 on Centos 7 

С. Возьмём этот образ https://github.com/kiwenlau/hadoop-cluster-docker
Ubuntu image with Hadoop, Spark, Kafka and HBase

$ sudo docker pull liliasfaxi/spark-hadoop:hv-2.7.2 
```

:white_check_mark: Проверить, что скачались
```php
$ sudo docker images
    REPOSITORY                TAG        IMAGE ID       CREATED       SIZE
    liliasfaxi/spark-hadoop   hv-2.7.2   d64a47823a96   3 years ago   1.94GB
    gvacaliuc/hadoop-docker   latest     dc2a19dec6b3   4 years ago   1.92GB
    cloudera/quickstart       latest     4239cd2958c6   6 years ago   6.34GB
```

### III. Создание контейнеров на базе docker-образа, создание пользовательской сети из трёх узлов 

:white_check_mark: Проверить контейнеры:
```php
$ docker ps -a   # все созданные контейнеры, но не запущенные
$ docker ps      # только запущенные контейнеры
```

:white_check_mark:  Создать пользовательскую Bridge-сеть, присвоить имя hadoop. Сеть Docker используется для установления связи между контейнерами Docker и внешним миром через хост-машину.
```php
$ docker network create --driver=bridge hadoop
```

:white_check_mark: Проверить, что появилась в списке
```php
$  docker network ls
    NETWORK ID     NAME      DRIVER    SCOPE
    17dafdae6518   bridge    bridge    local
    1e5c7e6b292c   hadoop    bridge    local
    5a63027d9ad1   host      host      local
    2bc81bc6fec8   none      null      local
```

Кластер HDFS состоит из главного сервера (namenode), который управляет пространством имен файловой системы и контролирует доступ к файлам. Другие узлы в кластере являются серверами datanodes, которые управляют хранилищем, подключенным к узлам, а также отвечают за создание/удаление/репликацию блоков по указанию namenodes.

:white_check_mark: Создать минимальный кластер из трёх узлов (каждый узел в отдельном контейнере) - Master-контейнер и два Slave-контейнера на основе образа liliasfaxi/spark-hadoop:hv-2.7.2, включить их в сеть hadoop и запустить (на Master открыть порты -p для входа в веб-интерфейсы: 
- порт 50070 - отображает информацию о нашем namenode,
- порт 8088 - информацию из диспетчера ресурсов Yarn и статусы различных заданий)
```php
$ docker run -itd --net=hadoop -p 50070:50070 -p 8088:8088 -p 7077:7077 -p 16010:16010 \
            --name hadoop-master --hostname hadoop-master \
            liliasfaxi/spark-hadoop:hv-2.7.2


$ docker run -itd -p 8040:8042 --net=hadoop \
        --name hadoop-slave1 --hostname hadoop-slave1 \
              liliasfaxi/spark-hadoop:hv-2.7.2


$ docker run -itd -p 8041:8042 --net=hadoop \
        --name hadoop-slave2 --hostname hadoop-slave2 \
              liliasfaxi/spark-hadoop:hv-2.7.2
```

:white_check_mark: Вывести список запущенных контейнеров
$ docker ps
```php
CONTAINER ID   IMAGE                              COMMAND                  CREATED             STATUS          PORTS                                                                                                                                                                                NAMES
494f1060e453   liliasfaxi/spark-hadoop:hv-2.7.2   "sh -c 'service ssh …"   14 minutes ago      Up 14 minutes   0.0.0.0:8041->8042/tcp, :::8041->8042/tcp                                                                                                                                            hadoop-slave2
88a387791742   liliasfaxi/spark-hadoop:hv-2.7.2   "sh -c 'service ssh …"   15 minutes ago      Up 14 minutes   0.0.0.0:8040->8042/tcp, :::8040->8042/tcp                                                                                                                                            hadoop-slave1
ffb57bce1c65   liliasfaxi/spark-hadoop:hv-2.7.2   "sh -c 'service ssh …"   16 minutes ago      Up 16 minutes   0.0.0.0:7077->7077/tcp, :::7077->7077/tcp, 0.0.0.0:8088->8088/tcp, :::8088->8088/tcp, 0.0.0.0:16010->16010/tcp, :::16010->16010/tcp, 0.0.0.0:50070->50070/tcp, :::50070->50070/tcp   hadoop-master
```

Если контейнеры уже были созданы, но не запущены (т.е. выводятся только в списке $ docker ps -a), запустить их
```php
$ docker start hadoop-master
$ docker start hadoop-slave1
$ docker start hadoop-slave2
```

:white_check_mark: Зайти в master-контейнер, режим командной строки bash
```php
$ docker exec -it hadoop-master bash
  root@hadoop-master:~#
```
:white_check_mark: Проверить работу сети - попробовать подключиться к любому из контейнеров и пропинговать два других, используя имя контейнера (предварительно установить в докере или при сборке образа пакет iputils-ping)
```php
$ docker container attach hadoop-slave1

root@hadoop-slave1:~# apt-get update
root@hadoop-slave1:~# apt-get install iputils-ping

root@hadoop-slave1:~# ping -c 2 hadoop-slave2
PING hadoop-slave2 (172.18.0.4) 56(84) bytes of data.
    64 bytes from hadoop-slave2.hadoop (172.18.0.4): icmp_seq=1 ttl=64 time=0.302 ms
    64 bytes from hadoop-slave2.hadoop (172.18.0.4): icmp_seq=2 ttl=64 time=0.173 ms
    --- hadoop-slave2 ping statistics ---
    2 packets transmitted, 2 received, 0% packet loss, time 1025ms
    rtt min/avg/max/mdev = 0.173/0.237/0.302/0.066 ms

root@hadoop-slave1:~# ping -c 2 hadoop-master
    PING hadoop-master (172.18.0.2) 56(84) bytes of data.
    64 bytes from hadoop-master.hadoop (172.18.0.2): icmp_seq=1 ttl=64 time=0.363 ms
    64 bytes from hadoop-master.hadoop (172.18.0.2): icmp_seq=2 ttl=64 time=0.184 ms
    --- hadoop-master ping statistics ---
    2 packets transmitted, 2 received, 0% packet loss, time 1023ms
    rtt min/avg/max/mdev = 0.184/0.273/0.363/0.091 ms
```

Пинги проходят, контейнеры могут общаться друг с другом. Вывести информацию о нашей сети можно с помощью команды inspect
```php
# docker network inspect hadoop
[
    {
        "Name": "hadoop",
        "Id": "1e5c7e6b292ca3d2936fe528a624497768c54fb4adf191028aa6fb2e6b3836a0",
        "Created": "2022-05-04T23:33:29.079172711+03:00",
        "Scope": "local",
        "Driver": "bridge",
        "EnableIPv6": false,
        "IPAM": {
            "Driver": "default",
            "Options": {},
            "Config": [
                {
                    "Subnet": "172.18.0.0/16",
                    "Gateway": "172.18.0.1"
                }
            ]
        },
        "Internal": false,
        "Attachable": false,
        "Ingress": false,
        "ConfigFrom": {
            "Network": ""
        },
        "ConfigOnly": false,
        "Containers": {
            "494f1060e453cca271a42ae7e8b15335e1307b62eecda769b2ae87851062038b": {
                "Name": "hadoop-slave2",
                "EndpointID": "b439e1397f70b24c6715c731d96522027659e1e9e3272060a890f9b2c832df89",
                "MacAddress": "02:42:ac:12:00:04",
                "IPv4Address": "172.18.0.4/16",
                "IPv6Address": ""
            },
            "ffb57bce1c6581e15a2f882828167a16103ced5d9d017cf88df9dea922311d5f": {
                "Name": "hadoop-master",
                "EndpointID": "84539b57deede50d2668e5e178ebde195f1232eb9c8176bc375c62e9e5bec997",
                "MacAddress": "02:42:ac:12:00:02",
                "IPv4Address": "172.18.0.2/16",
                "IPv6Address": ""
            }
        },
        "Options": {},
        "Labels": {}
    }
]
```
Убедиться, что сетевой интерфейс "docker0 для связи с созданными контейнерам, который Docker пытается автоматически сконфигурировать, поднят - имеет статус UP.
```php
larisa@larisa:~$ ip a sh docker0 
4: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN group default 
    link/ether 02:42:db:f7:18:d7 brd ff:ff:ff:ff:ff:ff
    inet 172.17.0.1/16 brd 172.17.255.255 scope global docker0
       valid_lft forever preferred_lft forever
```
 

### IV. Запуск hadoop-сервисов 

Пока не запущены все сервисы, мы не можем видеть список hdfs-файлов
```php
root@hadoop-master:~# hdfs dfs -ls /
Call From hadoop-master/172.18.0.2 to hadoop-master:9000 failed on connection exception: java.net.ConnectException: Connection refused; 
```
:white_check_mark:  Запустить скрипт start-hadoop.sh для запуска сервисов hadoop и yarn (namenode, datanodes, resourcemanager)
```php
root@hadoop-master:~# ./start-hadoop.sh

    Starting namenodes on [hadoop-master]
    hadoop-master: Warning: Permanently added 'hadoop-master,172.18.0.2' (ECDSA) to the list of known hosts.
    hadoop-master: starting namenode, logging to /usr/local/hadoop/logs/hadoop-root-namenode-hadoop-master.out
    hadoop-slave1: Warning: Permanently added 'hadoop-slave1,172.18.0.3' (ECDSA) to the list of known hosts.
    hadoop-slave2: Warning: Permanently added 'hadoop-slave2,172.18.0.4' (ECDSA) to the list of known hosts.
    hadoop-slave2: starting datanode, logging to /usr/local/hadoop/logs/hadoop-root-datanode-hadoop-slave2.out
    hadoop-slave1: starting datanode, logging to /usr/local/hadoop/logs/hadoop-root-datanode-hadoop-slave1.out
    Starting secondary namenodes [0.0.0.0]
    0.0.0.0: Warning: Permanently added '0.0.0.0' (ECDSA) to the list of known hosts.
    0.0.0.0: starting secondarynamenode, logging to /usr/local/hadoop/logs/hadoop-root-secondarynamenode-hadoop-master.out

    starting yarn daemons
    starting resourcemanager, logging to /usr/local/hadoop/logs/yarn--resourcemanager-hadoop-master.out
    hadoop-slave1: Warning: Permanently added 'hadoop-slave1,172.18.0.3' (ECDSA) to the list of known hosts.
    hadoop-slave2: Warning: Permanently added 'hadoop-slave2,172.18.0.4' (ECDSA) to the list of known hosts.
    hadoop-slave2: starting nodemanager, logging to /usr/local/hadoop/logs/yarn-root-nodemanager-hadoop-slave2.out
    hadoop-slave1: starting nodemanager, logging to /usr/local/hadoop/logs/yarn-root-nodemanager-hadoop-slave1.out
```
На master/slave-узлах должны запуститься следующие процессы
```php
root@hadoop-master:~# jps
163 NameNode
931 Jps
535 ResourceManager
364 SecondaryNameNode

larisa@larisa:~$ docker exec -it hadoop-slave2 bash
root@hadoop-slave2:~# jps
339 Jps
70 DataNode
187 NodeManager

larisa@larisa:~$ docker exec -it hadoop-slave1 bash
root@hadoop-slave1:~# jps
47 Jps
```

:white_check_mark:  Теперь можно вывести список каталогов нашего master-хоста:
```php
root@hadoop-master:~# ls -l
total 444000
drwxr-xr-x 1 root root      4096 Feb 22  2019 hdfs
-rw-r--r-- 1 root root 211312924 Feb  8  2017 purchases.txt
-rwxr-xr-x 1 root root       695 Mar  4  2018 run-wordcount.sh
-rwxr-xr-x 1 root root       120 Mar  4  2018 start-hadoop.sh
-rwxr-xr-x 1 root root       218 Mar  4  2018 start-kafka-zookeeper.sh

root@hadoop-master:~# cd hdfs
root@hadoop-master:~/hdfs# ls -l
total 12
drwxr-xr-x 2 root root 4096 Feb 22  2019 datanode
drwxr-xr-x 1 root root 4096 May  4 21:00 namenode
```

Отслеживать работу и поведение различных компонентов можно через консоль, а можно через веб-интерфейсы, которые предлагает Hadoop:
- интерфейс ResourceManager'а - http://localhost:8088/cluster/apps/, 
- hadoop-slave1 - http://localhost:8040, 
- hadoop-slave1 - http://localhost:8041.

:white_check_mark:  Как только кластер будет запущен и готов к использованию - в своем браузере на хост-компьютере перейти по адресам: http://localhost:50070,  http://localhost:8088, и проверить, что все ноды запущены, имеют Node state=RUNNING.
```php
http://localhost:8088/cluster/nodes
Node HTTP Address=hadoop-slave1:8042 RUNNING
Node HTTP Address=hadoop-slave2:8042 RUNNING
http://localhost:50070/dfshealth.html#tab-datanode
http://localhost:8040/node/node
http://localhost:8041/node/node
```


### V. Проверка распределённой работы системы MapReduce

Будем использовать файл purchases.txt(файл со списком покупок), который находится в главном каталоге master-машины, в качестве входных данных для обработки сервисом MapReduce. 

Следующие команды "hdfs dfs" будут относиться к фаловой системе hadoop (HDFS).

:white_check_mark: Создать каталог в HDFS с именем input
```php
root@hadoop-master:~# hdfs dfs -mkdir -p input
root@hadoop-master:~# hdfs dfs -ls 
  drwxr-xr-x   - root supergroup          0 2022-05-04 21:08 input
```

:white_check_mark: Загрузить тестовый файл purchases.txt в созданный входной каталог input:
```php
root@hadoop-master:~# hdfs dfs -put purchases.txt input
root@hadoop-master:~# hdfs dfs -ls -R  
    drwxr-xr-x   - root supergroup          0 2022-05-04 21:15 input
    -rw-r--r--   2 root supergroup  211312924 2022-05-04 21:15 input/purchases.txt
```

Можно вывести последние строки файла, или весь файл (но лучше этого не делать):
```php
root@hadoop-master:~# hdfs dfs -tail input/purchases.txt
31	17:59	Norfolk	Toys	164.34	MasterCard
2012-12-31	17:59	Chula Vista	Music	380.67	Visa
2012-12-31	17:59	Hialeah	Toys	115.21	MasterCard
2012-12-31	17:59	Indianapolis	Men's Clothing	158.28	MasterCard
2012-12-31	17:59	Norfolk	Garden	414.09	MasterCard
2012-12-31	17:59	Baltimore	DVDs	467.3	Visa
2012-12-31	17:59	Santa Ana	Video Games	144.73	Visa
2012-12-31	17:59	Gilbert	Consumer Electronics	354.66	Discover
2012-12-31	17:59	Memphis	Sporting Goods	124.79	Amex
2012-12-31	17:59	Chicago	Men's Clothing	386.54	MasterCard
2012-12-31	17:59	Birmingham	CDs	118.04	Cash
2012-12-31	17:59	Las Vegas	Health and Beauty	420.46	Amex
2012-12-31	17:59	Wichita	Toys	383.9	Cash
2012-12-31	17:59	Tucson	Pet Supplies	268.39	MasterCard
2012-12-31	17:59	Glendale	Women's Clothing	68.05	Amex
2012-12-31	17:59	Albuquerque	Toys	345.7	MasterCard
2012-12-31	17:59	Rochester	DVDs	399.57	Amex
2012-12-31	17:59	Greensboro	Baby	277.27	Discover
2012-12-31	17:59	Arlington	Women's Clothing	134.95	MasterCard
2012-12-31	17:59	Corpus Christi	DVDs	441.61	Discover

root@hadoop-master:~# hdfs dfs -cat input/purchases.txt
```

После загрузки файла, на страничке http://localhost:50070/dfshealth.html#tab-datanode можно увидеть, что все ноды задействованы в работе 
```php
In operation:
hadoop-slave1:50010 (172.18.0.3:50010)  Used=203.15 MB  Non DFS Used= 51.87 GB  Blocks=2   Block pool used=203.15MB(0.07%)
hadoop-slave2:50010 (172.18.0.4:50010)  Used=203.15 MB  Non DFS Used= 51.87 GB  Blocks=2   Block pool used=203.15MB(0.07%)
```

Если теперь вывести список файлов, увидим, что purchases.txt хранится на всех нодах
```php
root@hadoop-master:~/hdfs/datanode# hdfs dfs -ls -R
root@hadoop-master:/# hdfs dfs -ls -R
drwxr-xr-x   - root supergroup          0 2022-05-11 14:24 input
-rw-r--r--   2 root supergroup  211312924 2022-05-11 14:24 input/purchases.txt

root@hadoop-slave1:~# hdfs dfs -ls -R
drwxr-xr-x   - root supergroup          0 2022-05-11 14:24 input
-rw-r--r--   2 root supergroup  211312924 2022-05-11 14:24 input/purchases.txt

root@hadoop-slave2:~# hdfs dfs -ls -R
drwxr-xr-x   - root supergroup          0 2022-05-11 14:24 input
-rw-r--r--   2 root supergroup  211312924 2022-05-11 14:24 input/purchases.txt
```


<a name="222"><a>
B. ТЕСТ-ПЛАН СЕРВИСА HDFS
---------------------------------------------
Кластер HDFS включает следующие компоненты:
- управляющий узел NameNode - единственный в кластере, сервер для управления пространством имен файловой системы, хранящий дерево файлов, а также мета-данные файлов и каталогов. Отвечает за открытие и закрытие файлов, создание и удаление каталогов, управление доступом со стороны внешних клиентов и соответствие между файлами и блоками, дублированными (реплицированными) на узлах данных.
- несколько узлов DataNode – обязательный компонент кластера HDFS, который отвечает за запись и чтение данных, выполнение команд от узла NameNode по созданию, удалению и репликации блоков, а также периодическую отправку сообщения о состоянии (heartbeats) и обработку запросов на чтение и запись, поступающих от клиентов файловой системы HDFS.
-вторичный узел Secondary NameNode — отдельный сервер, единственный в кластере, который копирует образ HDFS и лог транзакций операций с файловыми блоками во временную папку, применяет изменения, накопленные в логе транзакций к образу HDFS, а также записывает его на узел NameNode и очищает лог транзакций.
- клиент Client – пользователь или приложение, взаимодействующий через специальный интерфейс (API – Application Programming Interface) с распределенной файловой системой. При наличии достаточных прав, клиенту разрешены следующие операции с файлами и каталогами: создание, удаление, чтение, запись, переименование и перемещение. 
- Standby Node          
- Checkpoint Node        
- Backup Node            

   
## I. Тестирование кластера
   
### I.1. Проверка работы с файловой системой HDFS
   
Допустимые файловые операции (кроме операций модификации, которые не поддерживается по причинам, связанным с архитектурными особенностями):
- Чтение списка файлов, директорий (ls)
- Чтение/запись файлов, чтение/создание директорий (text, cat, appendToFile, copyToLocal, moveFromLocal)
- Загрузка/выгрузка файлов (put, get, copyFromLocal)
- Управление файлами, каталогами (cp-копирование, rm/rmfir-удаление, mkdir/tochz-создание, mv-перемещение)
- Проверка файлов, смена прав доступа, владельца файлов (ckecksum, chmod, chown, chgrp)
- Проверка дискового пространства файловой системы (df, du)
- Администрирование (fsck-проверка и восстановление файловой системы, safemode-режим восстановления, format-форматирование namenode, balancer-запуск балансировщика между устройствами хранения)   
   
   
### I.2. Проверка репликации данных (хранение файлов в распределённой системе в виде нескольких копий на различных узлах)
   
- Ретрансляция файлового блока на другие узлы реплики (репликация осуществляются на уровне кластера в асинхронном режиме – информация распределяется по нескольким серверам прямо во время загрузки)
- Объём блока данных и количество создаваемых реплик (если клиент при создании файла не указал явно, то коэффициент репликации файловых блоков равен 3, размер блока файла 64 Мб)
- Расположение реплик (вторая реплика файлового блока хранится на другом узле, третья – на узле, расположенном на другой стойке, расположение следующих реплик вычисляется произвольно (механизм  rack awareness))
- Запуск репликации данных: 
    - a) при создании нового файла (операция записи)
    - b) в момент обнаружения сервером имен NameNode отказа одного из узлов данных, если NameNode не получает от DataNode heartbeat-сообщений
    - c) при повреждении существующих реплик
    - d) при увеличении количества реплик, присущих каждому блоку
- Журналирование операции репликации (окончание записи файлового блока фиксируется в журнале узла имен NameNode)   
   
   
### I.3. Возможность обработки различных форматов данных   

- Основные форматы хранения данных TXT, XML, JSON, AVRO, ORC, Parquet, Sequence файлы 
    - a) Неструктурированных- неорганизованные данные (например, видео)
    - b) Полуструктурированный- данные организованы в нефиксированном формате (например, JSON)
    - c) Структурированные- данные хранятся в структурированном формате (например, RDBMS)
- Методы обработки
    - a) пакетная обработка
    - b) потоковая обработка (загрузка данных через streaming-интерфейс Hadoop, Streaming Data Access)   

   
## II. Тестирование производительности
   
Тестирование производительности может включать в себя проверку:
- скорости обработки, пропускной способности данных
- скорости передачи соообщений и времени отклика
- количества операций в единицу времени     
- чтения, записи и обновления в базах данных в реальном времени
- времени выполнения задач
- использования памяти
- скорости обработки данных в соответствии с моделью MapReduce 
- времени ожидания соединения, времени ожидания запроса
- таймаутов чтения, записи
- балансировки распределения блоков по кластеру
- механизма распределённого кеширования    
   
   
## III. Тестирование отказоустойчивости
   
- Проверка бесперебойной обработки данных в случае отказа узлов данных
- Непрерывная доступность данных
- Стабильное соединение с различными потоками данных 
- Отчетность/логирование в режиме реального времени
- Проверка наличия ошибочных данных
- Возможность восстановления данных
   
   
## III. Тестирование безопасности
   
- Защита конфиденциальных данных (Data Security)
- Аутентификация и авторизация пользователей на основе ролей пользователей
- Шифрование данных и маскирование личной информации
   
   
<a name="333"><a>
С. АВТОТЕСТЫ
---------------------------------------------

Для автоматизации тестирования выбраны фреймворки:  
- a) TestNG - поддерживает параметризацию, тестирование на основе данных @dataProvider, расширенные возможности генерациии отчётов с помощью ReportNG,  
- b) Apаche.hadoop - библиотеки для разработки и выполнения распределённых программ.   
Пользователь взаимодействует с распределенной файловой системой через специальный API-интерфейс, по протоколам hdfs:// или http://. 
Код тестов см. в директории HadoopHdfsTest.   
