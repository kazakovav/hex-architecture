# Hexagonal Architecture / 3.2. Добавляем API списка и создания мастера

# Сервисный слой

В предыдущей части мы рассмотрели доменный слой нашей функциональности. Настало время переходить к сервисному слою.

## Организация кода и интерфейсов

В сети множество авторов предлагают совершенно разные схемы организации кода. Одни организуют код в виде классов сервисов, которые потом используются в контроллерах и других адаптерах. Другие предлагают все пользовательские сценарии бить на интерфейсы помещать их в пакет с названием `*.port.in`, а исходящие порты помещать в виде интерфейсов в пакет `*.port.out`, что явно указывает на входящие и исходящие порты. 

Наша структура и организация кода будет выглядеть следующим образом: 

```bash
ru.akazakov.beauty.application.expert
├── CreateExpertCommand.java
├── GetAllExpertsQuery.java
├── GetServiceItemsAvailableForExpertQuery.java
├── impl
│   ├── CreateExpertCommandImpl.java
│   ├── GetAllExpertsQueryImpl.java
│   └── GetServiceItemsAvailableForExpertQueryImpl.java
├── port
│   ├── GetAllExpertsPort.java
│   ├── GetAllServiceItemsPort.java
│   ├── GetServiceItemsPort.java
│   └── SaveExpertPort.java
└── request
    └── CreateExpertCommandRequest.java
```

В последнее время становится широко распостраненным подход, в котором функционал создания и изменения отделяется от фукнционала чтения, классами, интерфейсами или сервисами (да зравствует CQRS!). Будем использовать этот подход. Разместим в корне пакета интерфейсы, для основных бизнес сценариев нашего приложения - `CreateExpertCommandRequest`, `GetAllExpertsQuery`, `GetServiceItemsAvailableForExpertQuery`. Здесь `*Comand` обозначает, что сценарий подразумевает модификацию данных, а `*Query` - что данные запрашиваются для представленяния пользователю. 

Здесь `GetAllExpertsQuery`, `GetServiceItemsAvailableForExpertQuery` нужны лишь как вспомогательные сценарии, подробно на них останавливаться не будем, потому что в дальнейшем они вероятнее всего претерпят изменения. Данные интерфейсы были введены с целью проверки основного сценария создания **Эксперта**. 

Пакет `*.impl` содержит реализацию интерфейсов команд и запросов. В пакете `*.port` содержатся иходящие интерфейсы (порты), необходимые нашим службам для работы. Пакет request содержит запросы (DTO) необходимые службам, чтобы выполнить свою работу. `CreateExpertCommandRequest` - содержит необходимую информацию, которую команда `CreateExpertCommand` будет использовать для создания **Эксперта**.

## Бизнес сценарий (Сервис/Служба)

В Clean Architecture и Hexagonal Architecture да и в DDD в целом, основная обязанность сервиса - это выполнение важного бизнес-процесса, преобразование объекта предметной области путем изменения его состава. 

Как провести грань между сервисом и сущностью, кто из них должен реализовавывать ту или иную операцию? Во-первых все бизнес правила, которые являются общими для всей системы должны быть выражены в операциях **Сущности**. Во-вторых нужно задать себе вопрос, явлется ли данная операция основной для бизнес сценария (как например, привязка оказываемых услуг к сущности **Эксперта**) или вспомогательной (отправка уведомления, валидация входящих данных, сохранение/загрузка), в первом случае это обязанность **Сущности**, во втором **сервиса** или **службы**. В-третьих - все операции, которые не относятся к естественным обязанностям объекта **Сущности** должны быть вынесены в **сервис**(**службу**). 

Также в некоторых случаях - например операция сбора и чтения данных для предтавления пользователю (в нашем случае интерфейсы *Query) создание и использование дополнительной **Сущности** избыточно и не нужно и только усугубит понимание кода. В таких случаях допускается использование проекций данных и анемичных моделей - что то вроде View.

Опишем сценарий создания **Эксперта**:

1. Осуществляем проверку входных данных, на заполненность всех необходимых полей, если какие-либо поля не заполнены, бросаем исключение
2. Создаем **Эксперта**, используя входные данные
3. Проверяем что **Услуги**, которые указаны при создании **Эксперта** сущестуют в нашей системе и если они существуют приписываем **Эксперту** оказываемые **Услуги**
4. Сохраняем Нового **Эксперта**

Теперь реализуем это в коде:

```java
@Slf4j
@RequiredArgsConstructor
public class CreateExpertCommandImpl implements CreateExpertCommand {

    @Override
    @Transactional
    public Expert execute(CreateExpertCommandRequest request) {
        log.info("Create expert with request: {}", request);

        ConstraintViolations violations = validator.validate(request);
        if (!violations.isValid()) {
            log.error("Validation error: {}", violations);
            throw new IllegalArgumentException(
                    violations.stream().map(ConstraintViolation::message).toList().toString());
        }

        PersonalInfo personalInfo = buildPersonalInfo(request);
        ContactInfo contactInfo = buildContactInfo(request);
        TaxInfo taxInfo = buildTaxInfo(request);

        Expert expert = Expert.create(personalInfo, contactInfo, taxInfo);

        Set<ServiceItem> serviceItems = getServiceItems(request.getServiceItemsIds());

        if (CollectionUtils.isNotEmpty(serviceItems)) {
            log.info("Assigning service items to expert: {}", serviceItems);
            expert.assignServiceItems(serviceItems);
        }

        saveExpertPort.saveExpert(expert);

        return expert;
    }

}
```

На вход приходят данные для создания **Эксперта**, которые содержат всю необходимую информацию:

```java
@Getter
@Builder
@Jacksonized
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class CreateExpertCommandRequest {
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate birthDate;

    private String phone;
    private String email;

    private String individualTaxpayerNumber;

    private List<UUID> serviceItemsIds;

}
```

Далее нам необходимо проверить, что входящий запрос содержит корректную информацию, для этих целей также используется валидатор YAVI. Только в этом случае, он задается прямо в сервисе, потому что является частью бизнес логики:

```java
    public static Validator<CreateExpertCommandRequest> validator = ValidatorBuilder.<CreateExpertCommandRequest>of()
            .constraint(CreateExpertCommandRequest::getFirstName, CreateExpertCommandRequest.Fields.firstName,
                    c -> c.notBlank())
            .constraint(CreateExpertCommandRequest::getLastName, CreateExpertCommandRequest.Fields.lastName,
                    c -> c.notBlank())
            ._object(CreateExpertCommandRequest::getBirthDate, CreateExpertCommandRequest.Fields.birthDate,
                    c -> c.notNull())
            ._string(CreateExpertCommandRequest::getPhone, CreateExpertCommandRequest.Fields.phone,
                    CharSequenceConstraint::notBlank)
            ._string(CreateExpertCommandRequest::getIndividualTaxpayerNumber,
                    CreateExpertCommandRequest.Fields.individualTaxpayerNumber, CharSequenceConstraint::notBlank)
            .messageFormatter(new SimpleMessageFormatter())
            .build();
```

Ну и сам процесс проверки входящего запроса:

```java
...
        ConstraintViolations violations = validator.validate(request);
        if (!violations.isValid()) {
            log.error("Validation error: {}", violations);
            throw new IllegalArgumentException(
                    violations.stream().map(ConstraintViolation::message).toList().toString());
        }
...
```

Далее, так как информация валидна и достаточна для создания **Эксперта.** Мы конструируем нужную нам сущность, после создание производим назначение **Услуг** (если они были заданы) **Эксперту**:

```java
...
        Expert expert = Expert.create(personalInfo, contactInfo, taxInfo);

        Set<ServiceItem> serviceItems = getServiceItems(request.getServiceItemsIds());

        if (CollectionUtils.isNotEmpty(serviceItems)) {
            log.info("Assigning service items to expert: {}", serviceItems);
            expert.assignServiceItems(serviceItems);
        }
...        
```

Последний шаг - это сохранение **Эксперта**, здесь стоит обратить внимание, на то что аггрегат сохраняется полностью, со всеми компонентами, реализация сохранения производится в реализации порта `SaveExpertPort` (ее обсудим, когда будем добавлять БД к нашему сервису):

```java
...
        log.info("Save expert: {}", expert);
        saveExpertPort.saveExpert(expert);
...
```

Итоговый результат:

```java
@Slf4j
@RequiredArgsConstructor
public class CreateExpertCommandImpl implements CreateExpertCommand {
    private final GetServiceItemsPort getServiceItemsPort;
    private final SaveExpertPort saveExpertPort;

    public static Validator<CreateExpertCommandRequest> validator = ValidatorBuilder.<CreateExpertCommandRequest>of()
            .constraint(CreateExpertCommandRequest::getFirstName, CreateExpertCommandRequest.Fields.firstName,
                    c -> c.notBlank())
            .constraint(CreateExpertCommandRequest::getLastName, CreateExpertCommandRequest.Fields.lastName,
                    c -> c.notBlank())
            ._object(CreateExpertCommandRequest::getBirthDate, CreateExpertCommandRequest.Fields.birthDate,
                    c -> c.notNull())
            ._string(CreateExpertCommandRequest::getPhone, CreateExpertCommandRequest.Fields.phone,
                    CharSequenceConstraint::notBlank)
            ._string(CreateExpertCommandRequest::getIndividualTaxpayerNumber,
                    CreateExpertCommandRequest.Fields.individualTaxpayerNumber, CharSequenceConstraint::notBlank)
            .messageFormatter(new SimpleMessageFormatter())
            .build();

    @Override
    @Transactional
    public Expert execute(CreateExpertCommandRequest request) {
        log.info("Create expert with request: {}", request);

        ConstraintViolations violations = validator.validate(request);
        if (!violations.isValid()) {
            log.error("Validation error: {}", violations);
            throw new IllegalArgumentException(
                    violations.stream().map(ConstraintViolation::message).toList().toString());
        }

        PersonalInfo personalInfo = buildPersonalInfo(request);
        ContactInfo contactInfo = buildContactInfo(request);
        TaxInfo taxInfo = buildTaxInfo(request);

        Expert expert = Expert.create(personalInfo, contactInfo, taxInfo);

        Set<ServiceItem> serviceItems = getServiceItems(request.getServiceItemsIds());

        if (CollectionUtils.isNotEmpty(serviceItems)) {
            log.info("Assigning service items to expert: {}", serviceItems);
            expert.assignServiceItems(serviceItems);
        }

        log.info("Save expert: {}", expert);
        saveExpertPort.saveExpert(expert);

        return expert;
    }

    private Set<ServiceItem> getServiceItems(List<UUID> itemsUUIDs) {
        List<ServiceItemId> serviceItemsIds = itemsUUIDs.stream().map(id -> ServiceItemId.of(id))
                .toList();
        return getServiceItemsPort.getServiceItems(serviceItemsIds);
    }

    private TaxInfo buildTaxInfo(CreateExpertCommandRequest request) {
        TaxInfo taxInfo = TaxInfo.builder().individualTaxpayerNumber(request.getIndividualTaxpayerNumber()).build();
        return taxInfo;
    }

    private ContactInfo buildContactInfo(CreateExpertCommandRequest request) {
        ContactInfo contactInfo = ContactInfo.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
        return contactInfo;
    }

    private PersonalInfo buildPersonalInfo(CreateExpertCommandRequest request) {
        return PersonalInfo.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .birthDate(request.getBirthDate())
                .build();
    }

}
```

Переходим к тестам

## Тестирование Бизнес логики

Пока наши сервисы не содержат сложной и запутанной бизнес логики, мы можем ограничиться небольшими не сильно разветвленными юнит тестами. По мере роста сложности бизнес логики тесткейсы будут размножаться и содержать более сложные ветвления. В текущей ситуации команду создания **Эксперта** следующими сценариями:

1. Положительный сценарий, когда вся необходимая информация задана, но не заданы **Услуги**
2. Положительный сценария, когда вся необходимая информация задана и заданы **Услуги**, которые будет предоставлять **Эксперт**
3. Отрицательный сценарий - когда в запрос не добавили необходимой информации - номера телефона

Полный листинг кода теста: 

```java
@ExtendWith(MockitoExtension.class)
public class CreateExpertCommandImplTest {
    @Mock
    GetServiceItemsPort getServiceItemsPort;

    @Mock
    SaveExpertPort saveExpertPort;

    @Test
    void testExecuteSuccessAndServicesAreEmpty() {
        CreateExpertCommand createExpertCommand = new CreateExpertCommandImpl(getServiceItemsPort, saveExpertPort);

        List<UUID> serviceItemsIds = List.of(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));

        List<ServiceItemId> ids = serviceItemsIds.stream().map(v -> ServiceItemId.of(v)).toList();

        CreateExpertCommandRequest request = CreateExpertCommandRequest.builder()
                .firstName("Test")
                .lastName("Test")
                .birthDate(LocalDate.of(1999, 01, 01))
                .phone("+78888888888")
                .individualTaxpayerNumber("123-123-123")
                .serviceItemsIds(serviceItemsIds)
                .build();

        when(getServiceItemsPort.getServiceItems(ids)).thenReturn(Collections.emptySet());

        doNothing().when(saveExpertPort).saveExpert(any());

        createExpertCommand.execute(request);

        verify(saveExpertPort, times(1)).saveExpert(argThat(arg -> {
            var expert = (Expert) arg;
            return CollectionUtils.isEmpty(expert.getServices())
                    && Objects.nonNull(expert.getId())
                    && Objects.nonNull(expert.getId().getValue());
        }));
    }

    @Test
    void testExecuteSuccessAndServicesAreNotEmpty() {
        CreateExpertCommand createExpertCommand = new CreateExpertCommandImpl(getServiceItemsPort, saveExpertPort);

        List<UUID> serviceItemsIds = List.of(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));

        List<ServiceItemId> ids = serviceItemsIds.stream().map(v -> ServiceItemId.of(v)).toList();

        CreateExpertCommandRequest request = CreateExpertCommandRequest.builder()
                .firstName("Test")
                .lastName("Test")
                .birthDate(LocalDate.of(1999, 01, 01))
                .phone("+78888888888")
                .individualTaxpayerNumber("123-123-123")
                .serviceItemsIds(serviceItemsIds)
                .build();

        when(getServiceItemsPort.getServiceItems(ids)).thenReturn(Set.of(new ServiceItem(ids.get(0), "testService")));

        doNothing().when(saveExpertPort).saveExpert(any());

        createExpertCommand.execute(request);

        verify(saveExpertPort, times(1)).saveExpert(argThat(arg -> {
            var expert = (Expert) arg;
            return CollectionUtils.isNotEmpty(expert.getServices())
                    && Objects.nonNull(expert.getId())
                    && Objects.nonNull(expert.getId().getValue());
        }));
    }

    @Test
    void testExecuteFailedWithPhoneRequiredRequest() {
        CreateExpertCommand createExpertCommand = new CreateExpertCommandImpl(getServiceItemsPort, saveExpertPort);

        List<UUID> serviceItemsIds = List.of(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));

        CreateExpertCommandRequest request = CreateExpertCommandRequest.builder()
                .firstName("Test")
                .lastName("Test")
                .birthDate(LocalDate.of(1999, 01, 01))
                .individualTaxpayerNumber("123-123-123")
                .serviceItemsIds(serviceItemsIds)
                .build();

        assertThrows(IllegalArgumentException.class, () -> createExpertCommand.execute(request));
    }
}

```

Настало время перейти к слою данных и веб слою.

# Слой данных

Реализуя слой данных, мы можем воспользоваться основным и на мой взгляд главным преимуществом такой архитектуры - это независимость деталей реализации взаимодействия с внешним миром от бизнес логики. Более того, как и предлагает Роберт Мартин (Ancle Bob) мы, вначале разработки приложения, не будем погружаться в детали реализации хранилища наших сущностей и других компонентов, а воспользуемся сохранением всех элементов в память приложения или файл на диске. А после того, как наше приложение созреет, мы уже выберем реализацию хранилища. Это может быть как реляционная база данных, так и колоночная база данных или же NoSQL объектное хранилище типа Mongo. На более поздних этапах разработки нам с более высокой вероятностью будет понятно то, каким требованиям должна удовлетворять структура хранилища, в этот момент мы и выберем необходимую технологию  и структуру хранилища. Это позволит нам более эффективно использовать все преимущества той или иной технологии.

# Подключаем библиотеки

Как и описывалось выше, реализуем хранилище в виде файла на диске, просто для того, чтобы информация никуда не пропадала при перезапуске приложения. А поможет нам в этом библиотека - mapdb. 

Добавим соответствующую зависимость в `build.gradle`:

```groovy
...
    implementation 'org.mapdb:mapdb:3.1.0'
...    
```

Теперь реализуем в адаптере порты `SaveExpertPort`, `GetAllExpertsPort`. В данный момент не будем разбивать реализацию для каждого порта а имплементируем ее в одном адаптере. Все сущности **Экспертов** со всеми значениями будут храниться в одном файле. Пока не будем хранить отдельно сущности свзязи экспертов с **Услугами**. Итоговая реализация будет выглядеть так:

```java
@Slf4j
@Repository
public class ExpertAdapterInMemory implements SaveExpertPort, GetAllExpertsPort {
    private static final String DB_FILE_NAME = "experts.db";
    private static final String MAP_NAME = "experts";

    private final ExpertSerialzer expertSerialzer;

    public ExpertAdapterInMemory(ObjectMapper objectMapper) {
        this.expertSerialzer = new ExpertSerialzer(objectMapper);
    }

    @Override
    public void saveExpert(Expert expert) {
        log.info("Saving expert: {}", expert);
        try (DB db = DBMaker.fileDB(DB_FILE_NAME).make()) {
            HTreeMap<UUID, Expert> experts = db.hashMap(MAP_NAME).keySerializer(Serializer.UUID)
                    .valueSerializer(expertSerialzer).createOrOpen();

            experts.put(expert.getId().getValue(), expert);
        }
    }

    @Override
    public List<Expert> getAll() {
        log.info("Get all experts");
        try (DB db = DBMaker.fileDB(DB_FILE_NAME).make()) {
            HTreeMap<UUID, Expert> experts = db.hashMap(MAP_NAME).keySerializer(Serializer.UUID)
                    .valueSerializer(expertSerialzer).createOrOpen();

            return new ArrayList<>(experts.values());
        }
    }

    @RequiredArgsConstructor
    static class ExpertSerialzer extends GroupSerializerObjectArray<Expert> {
        private final ObjectMapper objectMapper;

        @Override
        public void serialize(DataOutput2 dataOutput2, Expert expert) throws IOException {
            dataOutput2.writeUTF(objectMapper.writeValueAsString(expert));
        }

        @Override
        public Expert deserialize(DataInput2 dataInput2, int i) throws IOException {
            return objectMapper.readValue(dataInput2.readUTF(), Expert.class);
        }
    }
}
```

Как и обсуждалось выше, на данном этапе нам достаточно такой реализации. Позже, когда мы финализируем до определенной степени наши сущности и бизнес логику, тогда уже добавим необходимое хранилище, соответствующее нашим требованиям

# Добавляем веб

Настало время для выставления во вне API эндпоинтов. Реализуем простой rest контроллер, который будет вызывать наши команды и отдавать результаты их работы. Код контроллера:

```java
@RestController
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
public class ExpertController {

    private final CreateExpertCommand createExpertCommand;

    private final GetAllExpertsQuery getAllExpertsQuery;

    private final GetServiceItemsAvailableForExpertQuery getServiceItemsAvailableForExpertQuery;

    @PostMapping
    public Expert create(@RequestBody CreateExpertCommandRequest createExpertCommandRequest) {
        Expert result = createExpertCommand.execute(createExpertCommandRequest);
        return result;
    }

    @GetMapping("/list")
    public List<Expert> getAllExperts() {
        return getAllExpertsQuery.execute();
    }

    @GetMapping("/service-items")
    public List<ServiceItem> getAvailableServiceItems() {
        return getServiceItemsAvailableForExpertQuery.execute();
    }

}
```

# Не забываем конфигурацию

Теперь, чтобы все заработало, нам необходимо определить необходимые spring бины и сконфигурировать их. Конечно это можно сделать через аннотации (`@Component`, `@Repository` и других), но в данном примере мы будем использовать явное определение бинов и их конфигурирование. Конфигурация, необходимая для создания **Эксперта**:

```java
@Configuration
public class ExpertConfig {
    @Bean
    public CreateExpertCommand createExpertCommand(GetServiceItemsPort getServiceItemsPort,
                                                   SaveExpertPort saveExpertPort) {
        return new CreateExpertCommandImpl(getServiceItemsPort, saveExpertPort);
    }

    @Bean
    public GetAllExpertsQuery getAllExpertsQuery(GetAllExpertsPort getAllExpertsPort) {
        return new GetAllExpertsQueryImpl(getAllExpertsPort);
    }

    @Bean
    public GetServiceItemsAvailableForExpertQuery getServiceItemsAvailableForExpertQuery(
            GetAllServiceItemsPort getAllServiceItemsPort) {
        return new GetServiceItemsAvailableForExpertQueryImpl(getAllServiceItemsPort);
    }
}
```

# Проверка сервиса

Запустим и проверим наш сервис. Стартуем spring-boot приложение:

```bash
akazakov@akazakov:~/Projects/blog-posts/hex-architecture/workspace/schedule$  /usr/bin/env /usr/lib/jvm/java-21-openjdk-amd64/bin/java -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=localhost:45085 @/tmp/cp_r5lxr0as2in4h8am0m3vkp6m.argfile ru.akazakov.beauty.infractructure.ScheduleApplication 
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.0)

2024-07-01T21:31:13.652+03:00  INFO 1394889 --- [           main] r.a.b.i.ScheduleApplication              : Starting ScheduleApplication using Java 21.0.3 with PID 1394889 (/home/akazakov/Projects/blog-posts/hex-architecture/workspace/schedule/infrastructure/build/classes/java/main started by akazakov in /home/akazakov/Projects/blog-posts/hex-architecture/workspace/schedule)
2024-07-01T21:31:13.667+03:00  INFO 1394889 --- [           main] r.a.b.i.ScheduleApplication              : No active profile set, falling back to 1 default profile: "default"
2024-07-01T21:31:14.634+03:00  INFO 1394889 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2024-07-01T21:31:14.646+03:00  INFO 1394889 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2024-07-01T21:31:14.647+03:00  INFO 1394889 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.24]
2024-07-01T21:31:14.715+03:00  INFO 1394889 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2024-07-01T21:31:14.717+03:00  INFO 1394889 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 970 ms
2024-07-01T21:31:14.882+03:00  INFO 1394889 --- [           main] r.a.b.d.s.GetServiceItemsAdapterInMemory : Initializing service items
2024-07-01T21:31:15.152+03:00  INFO 1394889 --- [           main] r.a.b.d.s.GetServiceItemsAdapterInMemory : Service items are contained in db: [ServiceItem(id=ServiceItemId(value=d947bd9b-2537-416f-a808-e25c05846ba8), name=haircut), ServiceItem(id=ServiceItemId(value=5e8ffd57-f644-4c0d-a202-d257e45f2299), name=manicure)]
2024-07-01T21:31:15.581+03:00  INFO 1394889 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2024-07-01T21:31:15.593+03:00  INFO 1394889 --- [           main] r.a.b.i.ScheduleApplication              : Started ScheduleApplication in 2.318 seconds (process running for 2.887)
```

Теперь воспользуемся rest client и попробуем создать Эксперта. Сначала получим список доступных Услуг:

```bash
### 
GET http://localhost:8080/api/v1/expert/service-items
Content-Type: application/json
```

В ответ получим, введенные нами услуги:

```bash
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 01 Jul 2024 18:34:28 GMT
Connection: close

[
  {
    "id": {
      "value": "d947bd9b-2537-416f-a808-e25c05846ba8"
    },
    "name": "haircut"
  },
  {
    "id": {
      "value": "5e8ffd57-f644-4c0d-a202-d257e45f2299"
    },
    "name": "manicure"
  }
]
```

Потом создадим эксперта с определенной Услугой:

```bash
###
POST http://localhost:8080/api/v1/expert
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Malkovich",
  "birthDate": "1991-06-13",
  "phone": "+78888888888",
  "individualTaxpayerNumber": "123-123-123",
  "serviceItemsIds": [
    "d947bd9b-2537-416f-a808-e25c05846ba8",
    "5e8ffd57-f644-4c0d-a202-d257e45f2299"
  ]
}
```

Результат работы метода: 

```bash
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 01 Jul 2024 18:36:10 GMT
Connection: close

{
  "id": {
    "value": "49945703-da95-4d6a-9cb5-096bce9f02a6"
  },
  "personalInfo": {
    "firstName": "John",
    "middleName": null,
    "lastName": "Malkovich",
    "birthDate": "1991-06-13"
  },
  "contactInfo": {
    "phone": "+78888888888",
    "email": null
  },
  "taxInfo": {
    "individualTaxpayerNumber": "123-123-123"
  },
  "services": [
    {
      "id": {
        "value": "d947bd9b-2537-416f-a808-e25c05846ba8"
      },
      "name": "haircut"
    },
    {
      "id": {
        "value": "5e8ffd57-f644-4c0d-a202-d257e45f2299"
      },
      "name": "manicure"
    }
  ]
}
```

И получим список всех экспертов, убедившись, что эксперт добавлен:

```bash
### 
GET http://localhost:8080/api/v1/expert/list
Content-Type: application/json

### Response

TTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 01 Jul 2024 18:37:00 GMT
Connection: close

[
  {
    "id": {
      "value": "49945703-da95-4d6a-9cb5-096bce9f02a6"
    },
    "personalInfo": {
      "firstName": "John",
      "middleName": null,
      "lastName": "Malkovich",
      "birthDate": "1991-06-13"
    },
    "contactInfo": {
      "phone": "+78888888888",
      "email": null
Что будем делать
Разобравшись с тем, как создавать многомодульное приложение при помощи gradle, пора приступить к добавлению фукнциональности в скелет приложения. Первым делом, добавим возможность получать список мастеров и список услуг, которые они предоставляют. 
Реализовывать функционал будем в следующей последовательности - от внутренних слоев к внешним. Таким образом мы сначала в первую очередь сосредоточимся на бизнес логике. Сначала создадим доменную модель, потом бизнес-логику (сервисы) и уже потом реализуем адаптеры.
Доменная модель
Моделируем объекты
Начнем моделирование нашей системы. В процессе развития и эволюции нашей системы доменные модели будут рождаться, изменяться и умирать. 
В текущей реализации, так как пример является обучающим/показательным, мы не  будем много времени посвящать бизнес моделированию доменной области и ограничимся лишь поверхностным анализом,  Однако, для того чтобы хорошо спроектировать и описать доменную модель и правильно разделить ее на сущности, агрегаты и значения, необходимо первоначально произвести глубокий анализ и моделирование процессов. Это позволит уже на первом этапе создать модели и правильно описать их основные свойства, а соответственно и написать тесты для этих сценариев, это позволит значительно уменьшить число ошибок в конечном продукте. Для бизнес моделирования придумано много методов и методик, например Event Storming (https://www.eventstorming.com)
Дальнейшее моделирование будет основано на моем оторванном от реальности предположении/понимании бизнес модели работы салонов красоты (🙂). 
Нужно полагать, что проектирование моделей а особенно на ранней стадии разработки и в условиях быстрого изменения требований к системе, никогда с первого раза не будет финальным. Поэтому в процессе развития системы модели могут дополняться, изменяться, разбиваться на несколько, собираться из нескольких и т.д. Поэтому не стоит относиться к ним как к постулату. Если модель требует изменения, то она должна измениться.
В походе Domain Driven Design сначала предлагается выбрать и отделить Смысловое Ядро системы - это то что приносит бизнесу деньги. Похоже, что в нашем случае смысловым ядром будет оказание Экспертом Услуг Клиенту. Но в рамках этой части мы возьмем вспомогательную сущность Эксперт, а работу со сложным смысловым ядром опишем уже после, когда разберемся во всех нюансах реализации гексагональной архитектуры.
Один из основных участников нашей системы - это мастер (Эксперт) предоставляющий определенные Услуги Клиентам. На текущем уровне нашего понимания системы, мы можем выделить 3 сущности в нашей системе - Эксперт, Услуга, Клиент. Сосредоточимся пока на первых двух. Какими свойствами они должны обладать:
Эксперт:
Идентификатор эксперта (ведь мы должны отличать экспертов в рамках системы)
Персональная информация - ФИО, дата рождения
Контактная информация - телефон, e
mail
Налоговая информация - инн, снилс, .
Услуга:
идентификатор услуги
наименование услуги
Выразим модели в коде
Услуга:
Java
Copy
Caption
@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@AllArgsConstructor
@RequiredArgsConstructor
public class ServiceItem {

    private final ServiceItemId id;

    private String name;

}

Услуга состоит пока только из 2-х полей - id и name.  Здесь идентификатор задается отдельным классом: 
Java
Copy
Caption
@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceItemId {
    private final UUID value;

    public static ServiceItemId of(UUID value) {
        return new ServiceItemId(value);
    }

    public static ServiceItemId create() {
        return of(UUID.randomUUID());
    }
}

Это сделано, для того, чтобы явно отделить идентификаторы конкретных сущностей от идентификаторов объектов-значений и остальных вспомогательных объектов. 
Помимо основных аннотаций lombok здесь пристствует аннотация @Jacksonized - эта аннотация вспомогательная, которая используется совместно с @Builder и @SuperBuilder, она автоматически конфигурирует builder для десериализации с помощью Jackson. 
Теперь перейдем к сущности Эксперта, пока очевидно, что некоторые свойства можно объединиться в отдельные классы значения, а Эксперт будет аггрегатом объединяющим их. Исходя из этих соображений разделим свойства на 3 класса:
PersonalInfo:
Java
Copy
Caption
@Getter
@Builder
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class PersonalInfo {
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate birthDate;
}

ContactInfo:
Java
Copy
Caption
@Getter
@Builder
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class ContactInfo {
    private String phone;
    private String email;
}

и TaxInfo:
Java
Copy
Caption
@Getter
@Builder
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class TaxInfo {
    private String individualTaxpayerNumber;
}

Соответственно наша сущность Эксперт будет выглядеть так:
Java
Copy
Caption
@Slf4j
@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@FieldNameConstants
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Expert {
    private final ExpertId id;

    private PersonalInfo personalInfo;

    private ContactInfo contactInfo;

    private TaxInfo taxInfo;

    private Set<ServiceItem> services;
}

Сейчас сущность содержит только  поля с информацией и является так называемой анемичной моделью - то есть той, которая несет в себе только данные, но не действия и правила. В таких случаях реализация всех правил возлагается на сервисный слой. В нашем же случае, наша будет содержать бизнес правила, которые свойственны ей. 
Добавляем бизнес правила
Первым делом, добавим методы создания новой сущности, они должны позволить создать Эксперта с указанием хотябы персональной и контактной информации и присвоить ему уникальный идентификатор:
Java
Copy
Caption
    public static Expert create(PersonalInfo personalInfo, ContactInfo contactInfo, TaxInfo taxInfo) {
        log.info("Creating new Expert, personalInfo: {}, contactInfo: {}, taxInfo: {}", personalInfo, contactInfo,
                taxInfo);
        return builder()
                .id(ExpertId.create())
                .personalInfo(personalInfo)
                .contactInfo(contactInfo)
                .taxInfo(taxInfo)
                .build();
    }

    public static Expert create(PersonalInfo personalInfo, ContactInfo contactInfo) {
        log.info("Creating new Expert, personalInfo: {}, contactInfo: {}", personalInfo, contactInfo);
        return builder()
                .id(ExpertId.create())
                .personalInfo(personalInfo)
                .contactInfo(contactInfo)
                .build();
    }

Здесь мы воспользуемся builder’ом а не конструктором по умолчанию по следующим причинам - необходимо проверить, что для создания Эксперта заполнили обязательные поля, если какое-либо поле не заполнено, то должно возникнуть исключение и не позволить создать сущность с недостаточными данными. Данная логика могла бы быть вынесена в отдельный метод и вызываться каждый раз, когда нам нужно, но чтобы избежать дублирования данного кода, вынесем его на этап построения объекта(builder), в этом случае мы еще и не позволим также десериализовать “некорректный” объект. Для этих целей переопределим builder, который создает lombok: 
Java
Copy
Caption
    public static ExpertBuilder builder() {
        return new ExpertBuilderWithValidation();
    }

    private static class ExpertBuilderWithValidation extends ExpertBuilder {

        @Override
        public Expert build() {
            Expert expert = super.build();
            ConstraintViolations violations = validator.validate(expert);
            if (!violations.isValid()) {
                throw new IllegalArgumentException(
                        violations.stream().map(ConstraintViolation::message).toList().toString());
            }
            return expert;
        }

    }

Прежде чем возвращать созданый объект, мы вызовем некоторый валидатор, который проверит поля объекта по определенным правилам и если нарушений не обнаружит, то вернет вновь созданный объект, либо бросит исключение. В качестве валидатора мы можем использовать механиз валидирования jakarta.validation (javax.validation) или написать свой. В последнее время мне нравится использовать YAVI (https://yavi.ik.am/) - он декларативный, функциональный и, на мой взгляд, более гибкий, по сравнению с механизмом аннотаций. Текущий валидатор:
Java
Copy
Caption
    public static Validator<Expert> validator = ValidatorBuilder.<Expert>of()
            ._object(Expert::getId, Fields.id, c -> c.notNull())
            ._object(Expert::getPersonalInfo, Fields.personalInfo, ObjectConstraint::notNull)
            .nestIfPresent(Expert::getPersonalInfo, Fields.personalInfo,
                    PersonalInfo.requireNameAndBirthDateValidator)
            ._object(Expert::getContactInfo, Fields.contactInfo, ObjectConstraint::notNull)
            .nestIfPresent(Expert::getContactInfo, Fields.contactInfo, ContactInfo.requirePhoneValidator)
            .nestIfPresent(Expert::getTaxInfo, Fields.taxInfo, TaxInfo.validator)
            .messageFormatter(new SimpleMessageFormatter())
            .build();

Валидатор помещен в объект сущности, наглядности, так видно все правила, которым подчиняется сущность. Но если правила валидации вырастут до больших размеров, то стоит выделить отдельный класс для валидатора.
Результат
Также в объект сущности Эксперт добавим метод, который позволяет Эксперту “присвоить” выполняемые услуги. Пока этот метод будет максимально простым. Итоговый класс будет выглядеть так: 
Java
Copy
Caption
@Slf4j
@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@FieldNameConstants
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Expert {
    private final ExpertId id;

    private PersonalInfo personalInfo;

    private ContactInfo contactInfo;

    private TaxInfo taxInfo;

    private Set<ServiceItem> services;

    public static Expert create(PersonalInfo personalInfo, ContactInfo contactInfo, TaxInfo taxInfo) {
        log.info("Creating new Expert, personalInfo: {}, contactInfo: {}, taxInfo: {}", personalInfo, contactInfo,
                taxInfo);
        return builder()
                .id(ExpertId.create())
                .personalInfo(personalInfo)
                .contactInfo(contactInfo)
                .taxInfo(taxInfo)
                .build();
    }

    public static Expert create(PersonalInfo personalInfo, ContactInfo contactInfo) {
        log.info("Creating new Expert, personalInfo: {}, contactInfo: {}", personalInfo, contactInfo);
        return builder()
                .id(ExpertId.create())
                .personalInfo(personalInfo)
                .contactInfo(contactInfo)
                .build();
    }

    public void assignServiceItems(Collection<ServiceItem> serviceItems) {
        if (services == null) {
            services = new HashSet<>();
        }
        services.addAll(serviceItems);
        log.info("New service items are added to expert, expertId: {}, serviceItems: {}", this.id, serviceItems);
    }

    public static ExpertBuilder builder() {
        return new ExpertBuilderWithValidation();
    }

    private static class ExpertBuilderWithValidation extends ExpertBuilder {

        @Override
        public Expert build() {
            Expert expert = super.build();
            ConstraintViolations violations = validator.validate(expert);
            if (!violations.isValid()) {
                throw new IllegalArgumentException(
                        violations.stream().map(ConstraintViolation::message).toList().toString());
            }
            return expert;
        }

    }

    public static Validator<Expert> validator = ValidatorBuilder.<Expert>of()
            ._object(Expert::getId, Fields.id, c -> c.notNull())
            ._object(Expert::getPersonalInfo, Fields.personalInfo, ObjectConstraint::notNull)
            .nestIfPresent(Expert::getPersonalInfo, Fields.personalInfo,
                    PersonalInfo.requireNameAndBirthDateValidator)
            ._object(Expert::getContactInfo, Fields.contactInfo, ObjectConstraint::notNull)
            .nestIfPresent(Expert::getContactInfo, Fields.contactInfo, ContactInfo.requirePhoneValidator)
            .nestIfPresent(Expert::getTaxInfo, Fields.taxInfo, TaxInfo.validator)
            .messageFormatter(new SimpleMessageFormatter())
            .build();

}

В валидаторе также используются некоторые объекты валидаторов из объектов значений входящих в состав агрегата (ContactInfo.requirePhoneValidator и TaxInfo.validator). С точки зрения OOD это и правильно и неправильно, с одной стороны исходя из нашей логики - каждый объект должен сам знать как его надо проверять, НО объекты-значения могут быть использованы в нескольких агрегатах, а в этом случае у каждого агрегата или сущности будут свои правила валидации этих объектов. В данном случае предлагается держать несколько объектов валидаторов в классах объектов-значений для удобства их группировки (или вынести в отдельный) и использовать тот, который актуален в данный момент. 
В результате всех действий с кодом мы получаем следующую структуру:
Bash
Copy
Caption
ru.akazakov.beauty.domain
├── common
│   ├── ContactInfo.java
│   ├── PersonalInfo.java
│   └── TaxInfo.java
├── expert
│   ├── ExpertId.java
│   └── Expert.java
└── service
    ├── ServiceItemId.java
    └── ServiceItem.java

Тестирование
Большим плюсом гексагональной архитектуры является ее тестируемость. Если код организован правильно, то будет не очень сложно написать юнит тесты. Добавим юнит тесты для нескольких сценариев создания Эксперта: 
Java
Copy
Caption
public class ExpertTest {
    @Test
    public void testCreatedSuccessful() {
        PersonalInfo personalInfo = PersonalInfo.builder()
                .firstName("John")
                .lastName("Smith")
                .birthDate(LocalDate.of(1985, 11, 11))
                .build();
        ContactInfo contactInfo = ContactInfo.builder().phone("+79999999999").build();
        TaxInfo taxInfo = TaxInfo.builder().individualTaxpayerNumber("111-1111-1111").build();
        Expert expert = Expert.create(personalInfo, contactInfo, taxInfo);

        assertNotNull(expert.getContactInfo());
        assertNotNull(expert.getPersonalInfo());
        assertNotNull(expert.getTaxInfo());
    }

    @Test
    void testCreateFailedWithPhoneMustBotBeBlankError() {

        PersonalInfo personalInfo = PersonalInfo.builder()
                .firstName("John")
                .lastName("Smith")
                .birthDate(LocalDate.of(1985, 11, 11))
                .build();
        ContactInfo contactInfo = ContactInfo.builder().email("aaaa@aaa.aa").build();
        TaxInfo taxInfo = TaxInfo.builder().individualTaxpayerNumber("111-1111-1111").build();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            Expert.create(personalInfo, contactInfo, taxInfo);
        });

        assertEquals("[\"contactInfo.phone\" must not be blank]", illegalArgumentException.getMessage());
    }

    @Test
    void testCreateFailedPersonalInfoIsNullError() {
        ContactInfo contactInfo = ContactInfo.builder().phone("+79999999999").build();
        TaxInfo taxInfo = TaxInfo.builder().individualTaxpayerNumber("111-1111-1111").build();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            Expert.create(null, contactInfo, taxInfo);
        });

        assertEquals("[\"personalInfo\" must not be null]", illegalArgumentException.getMessage());
    }
}

Настало время перейти на более верхний слой архитектуры - слой пользовательских сценариев.    },
    "taxInfo": {
      "individualTaxpayerNumber": "123-123-123"
    },
    "services": [
      {
        "id": {
          "value": "d947bd9b-2537-416f-a808-e25c05846ba8"
        },
        "name": "haircut"
      },
      {
        "id": {
          "value": "5e8ffd57-f644-4c0d-a202-d257e45f2299"
        },
        "name": "manicure"
      }
    ]
  },
  {
    "id": {
      "value": "30ef62ad-637b-4fd5-89cf-91238a815016"
    },
    "personalInfo": {
      "firstName": "John",
      "middleName": null,
      "lastName": "Doe",
      "birthDate": "1991-06-13"
    },
    "contactInfo": {
      "phone": "+78888888888",
      "email": null
    },
    "taxInfo": {
      "individualTaxpayerNumber": "123-123-123"
    },
    "services": null
  },
  {
    "id": {
      "value": "34fdba6a-7cd2-4287-93a3-5ed40937b36b"
    },
    "personalInfo": {
      "firstName": "John",
      "middleName": null,
      "lastName": "Malkovich",
      "birthDate": "1991-06-13"
    },
    "contactInfo": {
      "phone": "+78888888888",
      "email": null
    },
    "taxInfo": {
      "individualTaxpayerNumber": "123-123-123"
    },
    "services": null
  }
]
```

# Заключение

Надеюсь в этой части мне удалось показать основные преимущества гексагональной архитектуры - тестируемость бизнес правил, что позволит делать более качественные приложения. И независимость от деталей реализации, что на первых порах разработки системы позволит на не задумываться о проектировании некоторых аспектов системы, которые могут быть разработаны уже постфактум, когда нам это понадобиться и мы уже будем уверены в выборе той или иной технологии. В следующей части, попробуем добавить к нашему приложению API First подход и также раскроем один из недостатков гексагональной архитектуры - конвертация запросов и ответов между слоями. Код текущей части доступен в github [https://github.com/kazakovav/hex-architecture/tree/3_Add_first_functionality/workspace/schedule](https://github.com/kazakovav/hex-architecture/tree/3_Add_first_functionality/workspace/schedule)

Спасибо за внимание!