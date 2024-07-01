# Hexagonal Architecture / 2. Hexagonal architecture skeleton

Создадим каркас приложения, на основе которого будем показывать реализации гексагональной архитектуры.

> Немного о приложении, которое будем разрабатывать. В качестве идеи для приложения возьмем составление расписания и записи клиентов в салон красоты. Основной функционал приложения видится в следующем: составление расписания работы мастеров; запись к мастеру на процедуру; ведение карточек клиента; и многое другое.
> 

Код приложения будет разбиваться на отдельные изолированные модули, соответствующие различным элементам гексагональной архитектуры. Сборка проекта будет реализована с помощью Gradle. Для каждого модуля мы сделаем отдельный модуль Gradle. Структура проекта:

```bash
schedule
├── adapters
│   ├── db
│   │   ├── src
│   │   └── build.gradle
│   └── web
│       ├── src
│       └── build.gradle
├── application
│   ├── src
│   └── build.gradle
├── domain
│   ├── src
│   └── build.gradle
├── infrastructure
│   ├── src
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

Теперь более подробно о каждом модуле:

- Верхний уровень - **schedule** это сам проект. `build.gradle` конфигурирует сборку проекта, которая потом будет использоваться другими модулями.
- **domain** - здесь содержаться основные бизнес сущности нашего проекта и их логика. это самый внутренний слой нашего приложения.
- **application** - уровень бизнес логики или сервисный слой. Этот модуль использует только модуль domain. Здесь же находятся интерфесы портов (для связи с внешним миром).
- **adapters** - реализация портов. Здесь происходит непосредственное взаимодействие с базой данных, другими сервисами по различным протоколам, очередями сообщений. На этом уровне осуществляется только транспорт и построение необходимых объектов.
- **infrastructure** - здесь находятся конфигурационные файлы spring, liquibase и остальная обвязка для запуска приложения.

Рассмотрим более подробно некоторые из файлов сборки:

## **Родительский модуль**

Файл `settings.gradle` включает все модули для сборки проекта: 

```groovy
rootProject.name = 'schedule'

include 'domain'
include 'application'
include 'adapters:db'
include 'adapters:web'
include 'infrastructure'
```

Далее - `build.gradle` включает в себя общие настройки, управление версиями приложния и общую информацию для модулей проекта - секция subprojects содержит конфигурацию, которая применяется ко всем модулям:

```groovy
plugins {
    id 'io.spring.dependency-management' version '1.1.4'
}

subprojects {
    group = 'ru.akazakov.beauty'
    version = '0.0.1-SNAPSHOT'

    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'
    apply plugin: 'java-library'

    repositories {
        mavenCentral()
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${springVersion}")
        }
    }

    compileJava {
        sourceCompatibility = 19
        targetCompatibility = 19
    }
}
```

Важная часть файла сборки -  dependencyManagement этот плагин позволяет импортировать maven bom, для использования в качестве управления зависимостями. В данном случае мы импортируем зависимости spring-boot. 

## **Дочерние модули**

В качестве примера рассмотрим модуль `infrastructure`. Данные модуль представляет собой верхний слой приложения, здесь собираются вместе модели, бизнес логика, реализации портов и адаптеров, здесь же производится конфигурация spring-boot приложения.  Сам файл `build.gradle`:

```groovy
plugins {
    id "org.springframework.boot" version "${springVersion}"
}

dependencies {
    implementation project(':domain')
    implementation project(':application')
    implementation project(':adapters:db')
    implementation project(':adapters:web')
    implementation ('org.springframework.boot:spring-boot-starter-web')

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'

    testImplementation('org.springframework.boot:spring-boot-starter-test') {
        exclude group: 'junit' // excluding junit 4
    }
    testImplementation 'org.junit.jupiter:junit-jupiter-engine:5.10.2'
    testImplementation 'org.junit.platform:junit-platform-launcher:1.10.2'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.11.0'
    testImplementation 'com.tngtech.archunit:archunit:1.2.1'
    testImplementation 'de.adesso:junit-insights:1.1.0'
    
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:1.5.5.Final"

}

test {
    useJUnitPlatform()
    systemProperty 'de.adesso.junitinsights.enabled', 'true'
}

```

Здесь мы явно подключаем зависимости нашего проекта:

```groovy
    implementation project(':domain')
    implementation project(':application')
    implementation project(':adapters:db')
    implementation project(':adapters:web')
```

А также подключаем spring-boot-web, для реализации REST веб сервиса:

```groovy
implementation ('org.springframework.boot:spring-boot-starter-web')
```

В остальных модулях проекта конфигурация немного отличается, в зависимости от фунционала модуля. В данный момент, реализация функционала выполнена в виде заготовок, чтобы только проверить работу приложения и не более того, например в модуле `adapters.web` присутствует только один контроллер, который отвечает теми же данными, что и были в него отправлены:

```java
package ru.akazakov.beauty.web.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.akazakov.beauty.domain.Customer;
import ru.akazakov.beauty.service.customer.CreateCustomerUseCase;
import ru.akazakov.beauty.service.customer.CreateCustomerRequest;
import ru.akazakov.beauty.web.customer.dto.CreateCustomerDto;
import ru.akazakov.beauty.web.customer.dto.CustomerDto;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final CreateCustomerUseCase createCustomerUseCase;

    @PostMapping
    public CustomerDto create(@RequestBody CreateCustomerDto createCustomerDto) {
        Customer customer = createCustomerUseCase.execute(new CreateCustomerRequest().setName(createCustomerDto.getName()));
        return new CustomerDto().setId(customer.getId().getValue()).setName(customer.getName()).setCreated(customer.getCreated());
    }

}

```

## **Сборка и запуск приложения**

Сборка приложения осуществляется через интерфейс IDE или консольной командой `./gradlew build.` В случае удачной сборки, в консоль выведется следующая информация:

```bash
(base) akazakov@akazakov:~/.../schedule$ ./gradlew build

BUILD SUCCESSFUL in 664ms
13 actionable tasks: 13 up-to-date
```

Далее запустим приложение и проверим работу контроллера.

Запустим приложение, в случае, удачного запуска в выводе увидим следующий текст:

```bash

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2024-04-23T21:35:29.559+03:00  INFO 956100 --- [           main] r.a.b.i.ScheduleApplication              : Starting ScheduleApplication using Java 19.0.2 with PID 956100 (/home/akazakov/Projects/blog-posts/hex-architecture/workspace/schedule/infrastructure/build/classes/java/main started by akazakov in /home/akazakov/Projects/blog-posts/hex-architecture/workspace/schedule)
2024-04-23T21:35:29.561+03:00  INFO 956100 --- [           main] r.a.b.i.ScheduleApplication              : No active profile set, falling back to 1 default profile: "default"
2024-04-23T21:35:30.125+03:00  INFO 956100 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2024-04-23T21:35:30.134+03:00  INFO 956100 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2024-04-23T21:35:30.134+03:00  INFO 956100 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.20]
2024-04-23T21:35:30.183+03:00  INFO 956100 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2024-04-23T21:35:30.184+03:00  INFO 956100 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 595 ms
2024-04-23T21:35:30.456+03:00  INFO 956100 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path ''
2024-04-23T21:35:30.461+03:00  INFO 956100 --- [           main] r.a.b.i.ScheduleApplication              : Started ScheduleApplication in 1.091 seconds (process running for 1.449)
```

Протестируем наш контроллер, с помощью RestClient:

```bash
POST http://localhost:8080/api/v1/customer
Content-Type: application/json

{
  "name": "Bob"
}
```

В ответ должны получить что-то вроде этого:

```bash
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 23 Apr 2024 18:37:18 GMT
Connection: close

{
  "id": "fed9a72e-f92b-40a3-a62f-0358faabc74d",
  "name": "Bob",
  "created": "2024-04-23T21:37:18.714489495"
}
```

## **Заключение**

В этой части мы создали скелет проекта, для дальнейшего использования в развитии приложения, код проекта доступен в github: [https://github.com/kazakovav/hex-architecture/commit/f2e37625ed34fd67bdcd7d2ef64f7a13a4e7f486](https://github.com/kazakovav/hex-architecture/commit/f2e37625ed34fd67bdcd7d2ef64f7a13a4e7f486). 
В качестве системы сборки был использован gradle. Был создан многомодульный проект gradle, каждый модуль которого содержит свой компонент гексагональной архитектуры. 

В дальнейшем, используя этот скелет, мы будем развивать приложения для салонов красоты. 

## Список литературы

[Building a Multi-Module Spring Boot Application with Gradle](https://reflectoring.io/spring-boot-gradle-multi-module/)

[GitHub - thombergs/buckpal at multi-module](https://github.com/thombergs/buckpal/tree/multi-module)

[Declaring Dependencies between Subprojects](https://docs.gradle.org/current/userguide/declaring_dependencies_between_subprojects.html)

[Dependency Management Plugin](https://docs.spring.io/dependency-management-plugin/docs/current-SNAPSHOT/reference/html/#dependency-management-configuration-bom-import)