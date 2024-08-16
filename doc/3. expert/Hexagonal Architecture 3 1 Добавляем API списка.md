# Hexagonal Architecture / 3.1. Добавляем API списка и создания мастера

# Что будем делать

Разобравшись с тем, как создавать многомодульное приложение при помощи gradle, пора приступить к добавлению фукнциональности в скелет приложения. Первым делом, добавим возможность получать список мастеров и список услуг, которые они предоставляют. 
Реализовывать функционал будем в следующей последовательности - от внутренних слоев к внешним. Таким образом мы сначала в первую очередь сосредоточимся на бизнес логике. Сначала создадим доменную модель, потом бизнес-логику (сервисы) и уже потом реализуем адаптеры.

# Доменная модель

## Моделируем объекты

Начнем моделирование нашей системы. В процессе развития и эволюции нашей системы доменные модели будут рождаться, изменяться и умирать. 

> В текущей реализации, так как пример является обучающим/показательным, мы не  будем много времени посвящать бизнес моделированию доменной области и ограничимся лишь поверхностным анализом,  Однако, для того чтобы хорошо спроектировать и описать доменную модель и правильно разделить ее на сущности, агрегаты и значения, необходимо первоначально произвести глубокий анализ и моделирование процессов. Это позволит уже на первом этапе создать модели и правильно описать их основные свойства, а соответственно и написать тесты для этих сценариев, это позволит значительно уменьшить число ошибок в конечном продукте. Для бизнес моделирования придумано много методов и методик, например Event Storming ([https://www.eventstorming.com](https://www.eventstorming.com/))
> 

Дальнейшее моделирование будет основано на моем оторванном от реальности предположении/понимании бизнес модели работы салонов красоты (🙂). 

> Нужно полагать, что проектирование моделей а особенно на ранней стадии разработки и в условиях быстрого изменения требований к системе, никогда с первого раза не будет финальным. Поэтому в процессе развития системы модели могут дополняться, изменяться, разбиваться на несколько, собираться из нескольких и т.д. Поэтому не стоит относиться к ним как к постулату. Если модель требует изменения, то она должна измениться.
> 

В походе Domain Driven Design сначала предлагается выбрать и отделить Смысловое Ядро системы - это то что приносит бизнесу деньги. Похоже, что в нашем случае смысловым ядром будет оказание **Экспертом Услуг Клиенту**. Но в рамках этой части мы возьмем вспомогательную сущность Эксперт, а работу со сложным смысловым ядром опишем уже после, когда разберемся во всех нюансах реализации гексагональной архитектуры.

Один из основных участников нашей системы - это мастер (**Эксперт**) предоставляющий определенные **Услуги Клиентам.** На текущем уровне нашего понимания системы, мы можем выделить 3 сущности в нашей системе - **Эксперт, Услуга, Клиент**. Сосредоточимся пока на первых двух. Какими свойствами они должны обладать:

- **Эксперт**:
    - Идентификатор эксперта (ведь мы должны отличать экспертов в рамках системы)
    - Персональная информация - ФИО, дата рождения
    - Контактная информация - телефон, e
    - mail
    - Налоговая информация - инн, снилс, .
- **Услуга**:
    - идентификатор услуги
    - наименование услуги

## Выразим модели в коде

**Услуга**:

```java
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
```

**Услуга** состоит пока только из 2-х полей - `id` и `name`.  Здесь идентификатор задается отдельным классом: 

```java
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
```

Это сделано, для того, чтобы явно отделить идентификаторы конкретных сущностей от идентификаторов объектов-значений и остальных вспомогательных объектов. 

Помимо основных аннотаций lombok здесь пристствует аннотация `@Jacksonized` - эта аннотация вспомогательная, которая используется совместно с `@Builder` и `@SuperBuilder`, она автоматически конфигурирует `builder` для десериализации с помощью Jackson. 

Теперь перейдем к сущности **Эксперта**, пока очевидно, что некоторые свойства можно объединиться в отдельные классы значения, а **Эксперт** будет аггрегатом объединяющим их. Исходя из этих соображений разделим свойства на 3 класса:

**PersonalInfo:**

```java
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
```

**ContactInfo:**

```java
@Getter
@Builder
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class ContactInfo {
    private String phone;
    private String email;
}
```

и **TaxInfo:**

```java
@Getter
@Builder
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class TaxInfo {
    private String individualTaxpayerNumber;
}
```

Соответственно наша сущность **Эксперт** будет выглядеть так:

```java
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
```

Сейчас сущность содержит только  поля с информацией и является так называемой анемичной моделью - то есть той, которая несет в себе только данные, но не действия и правила. В таких случаях реализация всех правил возлагается на сервисный слой. В нашем же случае, наша будет содержать бизнес правила, которые свойственны ей. 

## Добавляем бизнес правила

Первым делом, добавим методы создания новой сущности, они должны позволить создать **Эксперта** с указанием хотябы персональной и контактной информации и присвоить ему уникальный идентификатор:

```java
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
```

Здесь мы воспользуемся builder’ом а не конструктором по умолчанию по следующим причинам - необходимо проверить, что для создания **Эксперта** заполнили обязательные поля, если какое-либо поле не заполнено, то должно возникнуть исключение и не позволить создать сущность с недостаточными данными. Данная логика могла бы быть вынесена в отдельный метод и вызываться каждый раз, когда нам нужно, но чтобы избежать дублирования данного кода, вынесем его на этап построения объекта(builder), в этом случае мы еще и не позволим также десериализовать “некорректный” объект. Для этих целей переопределим builder, который создает lombok: 

```java
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
```

Прежде чем возвращать созданый объект, мы вызовем некоторый валидатор, который проверит поля объекта по определенным правилам и если нарушений не обнаружит, то вернет вновь созданный объект, либо бросит исключение. В качестве валидатора мы можем использовать механиз валидирования jakarta.validation (javax.validation) или написать свой. В последнее время мне нравится использовать YAVI ([https://yavi.ik.am/](https://yavi.ik.am/)) - он декларативный, функциональный и, на мой взгляд, более гибкий, по сравнению с механизмом аннотаций. Текущий валидатор:

```java
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
```

Валидатор помещен в объект сущности, наглядности, так видно все правила, которым подчиняется сущность. Но если правила валидации вырастут до больших размеров, то стоит выделить отдельный класс для валидатора.

## Результат

Также в объект сущности **Эксперт** добавим метод, который позволяет **Эксперту** “присвоить” выполняемые услуги. Пока этот метод будет максимально простым. Итоговый класс будет выглядеть так: 

```java
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
 (1)
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
```

> В валидаторе также используются некоторые объекты валидаторов из объектов значений входящих в состав агрегата (`ContactInfo.requirePhoneValidator` и `TaxInfo.validator`). С точки зрения OOD это и правильно и неправильно, с одной стороны исходя из нашей логики - каждый объект должен сам знать как его надо проверять, НО объекты-значения могут быть использованы в нескольких агрегатах, а в этом случае у каждого агрегата или сущности будут свои правила валидации этих объектов. В данном случае предлагается держать несколько объектов валидаторов в классах объектов-значений для удобства их группировки (или вынести в отдельный) и использовать тот, который актуален в данный момент.
> 

В результате всех действий с кодом мы получаем следующую структуру:

```bash
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
```

## Тестирование

Большим плюсом гексагональной архитектуры является ее тестируемость. Если код организован правильно, то будет не очень сложно написать юнит тесты. Добавим юнит тесты для нескольких сценариев создания **Эксперта**: 

```java
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
```

Настало время перейти на более верхний слой архитектуры - слой пользовательских сценариев.

Настало время перейти на более верхний слой архитектуры - слой пользовательских сценариев. Как обычно код доступен в гилабе [https://github.com/kazakovav/hex-architecture/tree/3_Add_first_functionality/workspace/schedule](https://github.com/kazakovav/hex-architecture/tree/3_Add_first_functionality/workspace/schedule)

Во второй части разберем сервисный слой и слои данных и веб, а так же соберем все вместе.

Спасибо за внимание!