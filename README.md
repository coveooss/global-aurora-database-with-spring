# Global Aurora Database With Spring Boot

This is a small demo application showing how to use a Global Aurora database that has multiple clusters using Spring Boot.

A blog post explaining how it works can be found here:
[Using Amazon Aurora Global Databases With Spring](https://<ADD-MISSING-LINK>)

To launch this demo application you need to:
- Edit the [application.yml](src/main/resources/application.yml) to specify your username, password and database endpoints.
- Launch the main in [GlobalAuroraDatabaseWithSpringApplication.java](src/main/java/com/coveo/globalauroradatabase/GlobalAuroraDatabaseWithSpringApplication.java)

Once the app is running you can confirm that a connections to both data source was established by looking at the console logs.
You should see something like the following:
```
com.zaxxer.hikari.HikariDataSource       : Primary-HikariPool - Starting...
com.zaxxer.hikari.HikariDataSource       : Primary-HikariPool - Start completed.
org.hibernate.dialect.Dialect            : HHH000400: Using dialect: org.hibernate.dialect.MariaDB53Dialect
o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform]
j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
com.zaxxer.hikari.HikariDataSource       : Secondary-HikariPool - Starting...
com.zaxxer.hikari.HikariDataSource       : Secondary-HikariPool - Start completed.
o.s.b.a.e.web.EndpointLinksResolver      : Exposing 14 endpoint(s) beneath base path '/monitoring'
o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
```

You can also check the monitoring metrics at [http://127.0.0.1:8080/monitoring/metrics/hikaricp.connections](http://127.0.0.1:8080/monitoring/metrics/hikaricp.connections) and confirm that both data source are present.
