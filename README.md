# Database connection

A simple command line application to test JDBC connection with provided JDBC driver and print the result set of the given query.


## Build and run

```
mvn clean package
```

Then run the below command (make sure you have your JDBC driver downloaded),

```
java -cp target/db-1.0.jar:<jdbc-driver>.jar DB db.properties
```


## Configuration

Use the [db.properties](db.properties) file as a base. Check the comments in the property file for its usage.


## Example

Let's try with h2 database. Download [h2 database](http://www.h2database.com/html/main.html). Start h2 database.
And run below command to test the connection (the default configuration is configured for h2 database),

```
java -cp target/db-1.0.jar:/tmp/h2/bin/h2.jar DB db.properties
```
