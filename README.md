# Example Kafka Streams application with fan-in
```
 ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐ 
 │   strings   │   │    ints     │   │   doubles   │   │    longs    │ 
 └──────┬──────┘   └──────┬──────┘   └──────┬──────┘   └──────┬──────┘ 
        │                 │                 │                 │        
        │                 │                 │                 │        
        │                 │                 │                 │        
        │                 │                 │                 │        
        │                 │                 │                 │        
        │                 │     ╔═════╗     │                 │        
        │                 │     ║merge║     │                 │        
        │                 │     ╚═════╝     │                 │        
        └─────────────────┴────────┬────────┴─────────────────┘        
                                   │                                   
                                   │                                   
                                   │╔════════════════╗                 
                                   │║aggregate-by-key║                 
                                   │╚════════════════╝                 
                                   │                                   
                                   │                                   
                                   │                                   
                                   ▼                                   
                            ┌─────────────┐                            
                            │ merge-parts │                            
                            └──────┬──────┘                            
                                   │                                   
                                   │╔═════════════════════════════════╗
                                   │║filter : containsAllRequiredParts║
                                   │╚═════════════════════════════════╝
                                   │╔═════════════════════════════════╗
                                   │║        map : mergeParts         ║
                                   │╚═════════════════════════════════╝
                                   │                                   
                                   ▼                                   
                            ┌─────────────┐                            
                            │merge-results│                            
                            └─────────────┘                            
```

## How to run?
A docker compose file is provided that sets up the following infrastructure

- Zookeeper
- Kafka
- Schema Registry

as well as boots up the two main classes in this project

- FanInExample
- ExampleProducer

and a console consumer for looking at the output.

Running this constellation of applications can thus simple be done via
```bash
docker-compose up -d
```
after which you can look at the logging for a particular piece via
```bash
docker-compose logs -f avro_console_consumer
```
