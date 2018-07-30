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

## License

**DB test**

Copyright (C) 2018  Sridhar Gnanasekaran

Project Home: https://github.com/srid99/db-test

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


---


**Database Table Printer**

Copyright (C) 2014  Hami Galip Torun

Email: hamitorun@e-fabrika.net

Project Home: https://github.com/htorun/dbtableprinter

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
