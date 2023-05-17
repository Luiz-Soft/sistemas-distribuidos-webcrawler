## Requesitos
`java --version` ==> `openjdk 20.0.1`\
`mvn --version` ==> `Apache Maven 3.9.2`

---
## Obter projeto
`git clone https://github.com/Luiz-Soft/sistemas-distribuidos-webcrawler.git`

___
## Compile
Comece no diratorio da raiz do projeto

Em `website/pom.xml` altera a linha 77 para uma das seguintes:
- `<mainClass>cliente.Cliente<\mainClass>`
- `<mainClass>downloader.Downloader<\mainClass>`
- `<mainClass>indexstoragebarrels.IndexStorageBarrel<\mainClass>`
- `<mainClass>queue.Queue<\mainClass>`
- `<mainClass>search_module.SearchModule<\mainClass>`
- `<mainClass>website.Application<\mainClass>`

Corra os comandos:
```
cd website
mvn clean package
```

Muda o nome `/website/target/website-0.0.1-SNAPSHOT.jar` para:
- `/website/target/cliente.jar`\
- `/website/target/downloader.jar`\
- `/website/target/barrel.jar`\
- `/website/target/queue.jar`\
- `/website/target/search.jar`\
- `/website/target/website.jar`\
respetivamente

Corra o comando"\
```
cd target
```

---
## Uso

### queue.jar:
	java -jar queue.jar <"port"_to_host_on_local>

### search.jar:
	java -jar search.jar <"port"_to_host_on_local> <"ip:port"_of_queue> 

### cliente.jar:
	java -jar cliente.jar <"ip:port"_of_smi>

### downloader.jar:
	java -jar downloader.jar <"ip:port"_of_queue>

### barrel.jar:
	java -jar barrel.jar <"ip:port"_of_smi>

### website.jar:
	java -jar website.jar <"ip:port"_of_smi>

----
## Exemplo

queue => localhost:1099\
smi => localhost:1098

java -jar queue.jar 1099\
java -jar search.jar 1098 localhost:1099\
java -jar cliente.jar localhost:1098\
java -jar downloader.jar localhost:1099\
java -jar barrel.jar localhost:1098\
java -jar website.jar localhost:1098\
