create table SPRING_AI_CHAT_MEMORY
(
    conversation_id varchar(36)                                  not null,
    content         text                                         not null,
    type            enum ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') not null,
    timestamp       timestamp                                    not null
);

create index SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
    on SPRING_AI_CHAT_MEMORY (conversation_id, timestamp);

create table accounts
(
    id                      int auto_increment
        primary key,
    account_type            enum ('GOOGLE', 'SYSTEM')               null,
    created_at              datetime(6)                             null,
    email                   varchar(255) collate utf8mb4_unicode_ci null,
    is_active               bit                                     not null,
    is_banned               bit                                     not null,
    is_first_login          bit                                     not null,
    otp                     varchar(255)                            null,
    otp_generated_time      datetime(6)                             null,
    password                varchar(255)                            null,
    reset_token             varchar(255)                            null,
    reset_token_expiry_time datetime(6)                             null,
    role                    enum ('ADMIN', 'COACH', 'MEMBER')       null,
    username                varchar(255) collate utf8mb4_unicode_ci null,
    constraint UKk8h1bgqoplx0rkngj01pm1rgp
        unique (username),
    constraint UKn7ihswpy07ci568w34q0oi8he
        unique (email)
);

create table achievement
(
    id             int auto_increment
        primary key,
    condition_json json                                                         not null,
    created_at     datetime(6)                                                  null,
    description    varchar(255)                                                 null,
    icon           varchar(255)                                                 null,
    is_deleted     bit                                                          not null,
    name           varchar(255)                                                 null,
    type           enum ('ACTIVITY', 'FINANCE', 'PROGRESS', 'SOCIAL', 'STREAK') null,
    updated_at     datetime(6)                                                  null
);

create table coach
(
    id               int auto_increment
        primary key,
    avatar_url       varchar(255)                     null,
    bio              varchar(255)                     null,
    certificate_url  varchar(255)                     null,
    experience_years int                              not null,
    first_name       varchar(255)                     not null,
    gender           enum ('FEMALE', 'MALE', 'OTHER') null,
    last_name        varchar(255)                     not null,
    rating_avg       double                           not null,
    rating_count     int                              not null,
    specializations  varchar(255)                     null,
    account_id       int                              null,
    constraint UKinv78v3cs0p3g95o7d2jj1h3i
        unique (account_id),
    constraint FKc8dah4bryucbg070yitf98olc
        foreign key (account_id) references accounts (id)
);

create table conversation
(
    id              int auto_increment
        primary key,
    created_at      datetime(6)                  null,
    last_updated_at datetime(6)                  null,
    title           varchar(255)                 null,
    type            varchar(20) default 'DIRECT' not null
);

create table form_metric
(
    id                                int auto_increment
        primary key,
    amount_of_nicotine_per_cigarettes decimal(38, 2) null,
    cigarette_hate_to_give_up         bit            not null,
    cigarettes_per_package            int            not null,
    created_at                        datetime(6)    not null,
    estimated_money_saved_on_plan     decimal(38, 2) null,
    estimated_nicotine_intake_per_day decimal(38, 2) null,
    interests                         json           null,
    minutes_after_waking_to_smoke     int            not null,
    money_per_package                 decimal(38, 2) null,
    morning_smoking_frequency         bit            not null,
    number_of_years_of_smoking        int            not null,
    smoke_avg_per_day                 int            not null,
    smoke_when_sick                   bit            not null,
    smoking_in_forbidden_places       bit            not null,
    triggered                         json           null,
    updated_at                        datetime(6)    not null
);

create table interest_category
(
    id          int auto_increment
        primary key,
    created_at  datetime(6)  null,
    description varchar(255) null,
    name        varchar(255) null,
    updated_at  datetime(6)  null
);

create table member
(
    id                    int auto_increment
        primary key,
    avatar_url            varchar(255)                     null,
    dob                   date                             null,
    first_name            varchar(255)                     not null,
    gender                enum ('FEMALE', 'MALE', 'OTHER') null,
    is_used_free_trial    bit                              not null,
    last_name             varchar(255)                     not null,
    modified_at           datetime(6)                      null,
    morning_reminder_time time(6)                          null,
    quiet_end             time(6)                          null,
    quiet_start           time(6)                          null,
    time_zone             varchar(255)                     null,
    account_id            int                              null,
    constraint UKi54h1gvvnejys85e9d9qo9f2u
        unique (account_id),
    constraint FKndx5tyrwupjpuptsppnbp3ybb
        foreign key (account_id) references accounts (id)
);

create table diary_record
(
    id                        int auto_increment
        primary key,
    anxiety_level             int            not null,
    cigarettes_smoked         int            not null,
    confidence_level          int            not null,
    craving_level             int            not null,
    created_at                datetime(6)    null,
    date                      date           null,
    estimated_nicotine_intake decimal(38, 2) null,
    have_smoked               bit            not null,
    heart_rate                int            not null,
    is_connect_iotdevice      bit            not null,
    is_use_nrt                bit            not null,
    money_spent_on_nrt        double         not null,
    mood_level                int            not null,
    note                      varchar(255)   null,
    reduction_percentage      double         not null,
    sleep_duration            double         not null,
    spo2                      int            not null,
    steps                     int            not null,
    triggers                  varbinary(255) null,
    updated_at                datetime(6)    null,
    member_id                 int            null,
    constraint FKl1ea6yrgt4x3gi0xcnmjmka7x
        foreign key (member_id) references member (id)
);

create table health_recovery
(
    id             int auto_increment
        primary key,
    description    varchar(255)                                                                                                                                                                                                                                                                                                                                    null,
    name           enum ('BREATHING', 'CARBON_MONOXIDE_LEVEL', 'CIRCULATION', 'DECREASED_RISK_OF_HEART_ATTACK', 'DECREASED_RISK_OF_LUNG_CANCER', 'ENERGY_LEVEL', 'GUMS_AND_TEETH', 'GUM_TEXTURE', 'IMMUNITY_AND_LUNG_FUNCTION', 'NICOTINE_EXPELLED_FROM_BODY', 'OXYGEN_LEVEL', 'PULSE_RATE', 'REDUCED_RISK_OF_HEART_DISEASE', 'TASTE_AND_SMELL', 'TOOTH_STAINING') null,
    recovery_time  double                                                                                                                                                                                                                                                                                                                                          not null,
    target_time    datetime(6)                                                                                                                                                                                                                                                                                                                                     null,
    time_triggered datetime(6)                                                                                                                                                                                                                                                                                                                                     null,
    value          decimal(5, 2)                                                                                                                                                                                                                                                                                                                                   null,
    member_id      int                                                                                                                                                                                                                                                                                                                                             null,
    constraint FK39l0yrix3mmoaccfhh489l1a4
        foreign key (member_id) references member (id)
);

create table member_achievement
(
    id             int auto_increment
        primary key,
    achieved_at    datetime(6) null,
    achievement_id int         null,
    member_id      int         null,
    constraint FKjvb236q9g873en441229sh2oa
        foreign key (member_id) references member (id),
    constraint FKqg0ihr3d0c7ff67oa7r6pdiej
        foreign key (achievement_id) references achievement (id)
);

create table membership_package
(
    id            int auto_increment
        primary key,
    description   varchar(255)                          null,
    duration      int                                   not null,
    duration_unit enum ('DAY', 'MONTH')                 null,
    features      varbinary(255)                        null,
    name          varchar(255)                          null,
    price         bigint                                not null,
    type          enum ('PREMIUM', 'STANDARD', 'TRIAL') null
);

create table membership_subscription
(
    id                    int auto_increment
        primary key,
    created_at            datetime(6)                                             null,
    end_date              date                                                    null,
    order_code            bigint                                                  not null,
    start_date            date                                                    null,
    status                enum ('AVAILABLE', 'EXPIRED', 'PENDING', 'UNAVAILABLE') null,
    total_amount          bigint                                                  not null,
    member_id             int                                                     null,
    membership_package_id int                                                     null,
    constraint FK2qvl1njcubdlw7jgq2mhnrykg
        foreign key (member_id) references member (id),
    constraint FK5hgxc7t23dx7q8qqwh8kycg95
        foreign key (membership_package_id) references membership_package (id)
);

create table message
(
    id              int auto_increment
        primary key,
    content         text                       null,
    is_deleted      bit                        not null,
    is_read         bit                        not null,
    message_type    varchar(20) default 'TEXT' not null,
    sent_at         datetime(6)                null,
    conversation_id int                        not null,
    sender_id       int                        not null,
    constraint FK3qmb16493gxe92xsdsqdrqvby
        foreign key (sender_id) references accounts (id),
    constraint FK6yskk3hxw5sklwgi25y6d5u1l
        foreign key (conversation_id) references conversation (id)
);

create table attachment
(
    id             int auto_increment
        primary key,
    attachment_url varchar(255) null,
    message_id     int          not null,
    constraint FKoo11928qbsiolkc10dph1p214
        foreign key (message_id) references message (id)
);

create table metric
(
    id                           int auto_increment
        primary key,
    annual_saved                 decimal(38, 2) null,
    avg_anxiety                  double         not null,
    avg_cigarettes_per_day       double         not null,
    avg_confident_level          double         not null,
    avg_craving_level            double         not null,
    avg_mood                     double         not null,
    avg_nicotine_mg_per_day      double         not null,
    comment_count                int            not null,
    completed_all_mission_in_day int            not null,
    created_at                   datetime(6)    null,
    current_anxiety_level        int            not null,
    current_confidence_level     int            not null,
    current_craving_level        int            not null,
    current_mood_level           int            not null,
    heart_rate                   int            not null,
    money_saved                  decimal(38, 2) null,
    post_count                   int            not null,
    reduction_percentage         double         not null,
    relapse_count_in_phase       int            not null,
    sleep_duration               double         not null,
    smoke_free_day_percentage    double         not null,
    spo2                         int            not null,
    steps                        int            not null,
    streaks                      int            not null,
    total_mission_completed      int            not null,
    updated_at                   datetime(6)    null,
    member_id                    int            null,
    reduction_in_last_smoked     double         not null,
    constraint UK20rn8tpus8afi6sljvtcq03lu
        unique (member_id),
    constraint FKo79v5uxq9mvg9o26csw0fx3n0
        foreign key (member_id) references member (id)
);

create table mission_type
(
    id          int auto_increment
        primary key,
    description varchar(255) null,
    name        varchar(255) null
);

create table mission
(
    id                   int auto_increment
        primary key,
    code                 varchar(255)                                                              null,
    condition_json       json                                                                      null,
    created_at           datetime(6)                                                               null,
    description          varchar(255)                                                              null,
    exp                  int                                                                       not null,
    name                 varchar(255)                                                              null,
    phase                enum ('MAINTENANCE', 'ONSET', 'PEAK_CRAVING', 'PREPARATION', 'SUBSIDING') null,
    status               enum ('ACTIVE', 'INACTIVE', 'INUSE', 'UNUSE')                             null,
    updated_at           datetime(6)                                                               null,
    interest_category_id int                                                                       null,
    mission_type_id      int                                                                       null,
    constraint UKtio2ulw4k2037685uaayxtuub
        unique (code),
    constraint FK54wu77t260551yemvh9bgwi0m
        foreign key (interest_category_id) references interest_category (id),
    constraint FKeur3cml0f4dn4utdeqpw334xe
        foreign key (mission_type_id) references mission_type (id)
);

create table news
(
    id            int auto_increment
        primary key,
    content       text                                 null,
    created_at    datetime(6)                          null,
    status        enum ('DELETED', 'DRAFT', 'PUBLISH') null,
    thumbnail_url varchar(255)                         null,
    title         varchar(255)                         null
);

create table news_media
(
    id         int auto_increment
        primary key,
    media_type enum ('IMAGE', 'VIDEO') null,
    media_url  varchar(255)            null,
    news_id    int                     null,
    constraint FKfh1dj1fd8dkxyfdxnq3hp4enk
        foreign key (news_id) references news (id)
);

create table notification
(
    id                int auto_increment
        primary key,
    content           text                                                                                                                                               null,
    created_at        datetime(6)                                                                                                                                        null,
    deep_link         varchar(255)                                                                                                                                       null,
    icon              varchar(255)                                                                                                                                       null,
    is_deleted        bit                                                                                                                                                not null,
    is_read           bit                                                                                                                                                not null,
    notification_type enum ('ACHIEVEMENT', 'APPOINTMENT_BOOKED', 'APPOINTMENT_CANCELLED', 'APPOINTMENT_REMINDER', 'MISSION', 'PHASE', 'QUIT_PLAN', 'REMINDER', 'SYSTEM') null,
    title             varchar(255)                                                                                                                                       null,
    url               varchar(255)                                                                                                                                       null,
    account_id        int                                                                                                                                                null,
    constraint FKawh4fe2xxmss39y0r8eydjtq4
        foreign key (account_id) references accounts (id)
);

create table participant
(
    id              int auto_increment
        primary key,
    last_read_at    datetime(6) null,
    account_id      int         not null,
    conversation_id int         not null,
    constraint UKfh7tjbx3f45m1s5wg2f9hog4i
        unique (account_id, conversation_id),
    constraint FKqr4116w44nlc9i9cg6f1i31
        foreign key (account_id) references accounts (id),
    constraint FKsiftd56p4vnlfthffmf07xhng
        foreign key (conversation_id) references conversation (id)
);

create table payment
(
    id              int auto_increment
        primary key,
    amount          bigint                                                                  not null,
    created_at      datetime(6)                                                             null,
    order_code      bigint                                                                  not null,
    payment_link_id varchar(255)                                                            null,
    status          enum ('CANCELLED', 'COMPLETED', 'FAILED', 'PAID', 'PENDING', 'SUCCESS') null,
    subscription_id int                                                                     null,
    constraint UKleu7cidc9pohwh5bierp7cpmw
        unique (subscription_id),
    constraint FK1kkyq2ddcqf57clcsq97deox9
        foreign key (subscription_id) references membership_subscription (id)
);

create table post
(
    id          int auto_increment
        primary key,
    content     text                          null,
    created_at  datetime(6)                   null,
    description varchar(255)                  null,
    status      enum ('DELETED', 'PUBLISHED') null,
    thumbnail   varchar(255)                  null,
    title       varchar(255)                  null,
    updated_at  datetime(6)                   null,
    account_id  int                           not null,
    constraint FKcn5lafwqd9x8dmcbnpk4hqhrs
        foreign key (account_id) references accounts (id)
);

create table comment
(
    id         int auto_increment
        primary key,
    content    text        null,
    created_at datetime(6) null,
    updated_at datetime(6) null,
    account_id int         null,
    parent_id  int         null,
    post_id    int         not null,
    constraint FK1foibrrqj7968wu83tdmwrqig
        foreign key (account_id) references accounts (id),
    constraint FKde3rfu96lep00br5ov0mdieyt
        foreign key (parent_id) references comment (id),
    constraint FKs1slvnkuemjsq2kj4h3vhx7i1
        foreign key (post_id) references post (id)
);

create table comment_media
(
    id         int auto_increment
        primary key,
    media_type enum ('IMAGE', 'VIDEO') null,
    media_url  varchar(255)            null,
    comment_id int                     not null,
    constraint FKa621yp9hnl2oh2u355txqk9h8
        foreign key (comment_id) references comment (id)
);

create table post_media
(
    id         int auto_increment
        primary key,
    media_type enum ('IMAGE', 'VIDEO') null,
    media_url  varchar(255)            null,
    post_id    int                     null,
    constraint FKo5e3or8sh0maaq8jy948d3tf9
        foreign key (post_id) references post (id)
);

create table quit_plan
(
    id             int auto_increment
        primary key,
    created_at     datetime(6)                                              not null,
    end_date       date                                                     null,
    ftnd_score     int                                                      not null,
    is_active      bit                                                      not null,
    name           varchar(255)                                             null,
    start_date     date                                                     null,
    status         enum ('CANCELED', 'COMPLETED', 'CREATED', 'IN_PROGRESS') null,
    updated_at     datetime(6)                                              not null,
    use_nrt        bit                                                      null,
    form_metric_id int                                                      null,
    member_id      int                                                      null,
    constraint UK44wjh0vj629r7u51avrp8luoa
        unique (form_metric_id),
    constraint FKfrijypsa2g8ciqlrohk40uvvo
        foreign key (member_id) references member (id),
    constraint FKnqt45vgbvw15bouonsi8h13f
        foreign key (form_metric_id) references form_metric (id)
);

create table reminder_template
(
    id            int auto_increment
        primary key,
    content       text                                                                      null,
    created_at    datetime(6)                                                               null,
    phase_enum    enum ('MAINTENANCE', 'ONSET', 'PEAK_CRAVING', 'PREPARATION', 'SUBSIDING') null,
    reminder_type enum ('BEHAVIOR', 'MORNING', 'SMOKED')                                    null,
    trigger_code  varchar(255)                                                              null,
    updated_at    datetime(6)                                                               null
);

create table slot
(
    id         int auto_increment
        primary key,
    end_time   time(6) null,
    start_time time(6) null
);

create table coach_work_schedule
(
    id       int auto_increment
        primary key,
    date     date                                        null,
    status   enum ('AVAILABLE', 'BOOKED', 'UNAVAILABLE') null,
    coach_id int                                         null,
    slot_id  int                                         null,
    constraint FK5tv2o6uv9y7offtyq5plgfkw0
        foreign key (slot_id) references slot (id),
    constraint FKsr9qocppsky3w37phes3t9fwp
        foreign key (coach_id) references coach (id)
);

create table appointment
(
    id                     int auto_increment
        primary key,
    appointment_status     enum ('CANCELLED', 'COMPLETED', 'IN_PROGRESS', 'PENDING') null,
    cancelled_at           datetime(6)                                               null,
    cancelled_by           enum ('COACH', 'MEMBER')                                  null,
    created_at             datetime(6)                                               null,
    date                   date                                                      null,
    name                   varchar(255)                                              null,
    coach_id               int                                                       null,
    coach_work_schedule_id int                                                       null,
    member_id              int                                                       null,
    constraint FKdbsjt9114v0t55d97yv1paneh
        foreign key (member_id) references member (id),
    constraint FKdcam1ndxrp9qwd4ni5bab67c0
        foreign key (coach_work_schedule_id) references coach_work_schedule (id),
    constraint FKjyxm2c69jwut5lag9saw0rd8u
        foreign key (coach_id) references coach (id)
);

create table appointment_snapshots
(
    appointment_id int          not null,
    image_url      varchar(255) null,
    constraint FK56jxeaqir3e6jqy7u5tv4pwlq
        foreign key (appointment_id) references appointment (id)
);

create table feedback
(
    id             int auto_increment
        primary key,
    content        text        null,
    created_at     datetime(6) not null,
    star           int         not null,
    appointment_id int         not null,
    coach_id       int         not null,
    member_id      int         not null,
    constraint UKeicffywgx67t4cyky7tckf3h
        unique (appointment_id),
    constraint FK9wfgrg1xqrkfk32mwisl37o0v
        foreign key (appointment_id) references appointment (id),
    constraint FKjacylwmm6sn5uf8n1tlt0mjvk
        foreign key (coach_id) references coach (id),
    constraint FKmonjtjt92g6gruqyfumtmg8m8
        foreign key (member_id) references member (id)
);

create table system_phase_condition
(
    id             int auto_increment
        primary key,
    condition_json json         not null,
    name           varchar(255) null,
    updated_at     datetime(6)  not null
);

create table phase
(
    id                        int auto_increment
        primary key,
    avg_anxiety               double                                                 not null,
    avg_cigarettes_per_day    double                                                 not null,
    avg_confident_level       double                                                 not null,
    avg_craving_level         double                                                 not null,
    avg_mood                  double                                                 not null,
    completed_at              datetime(6)                                            null,
    completed_missions        int                                                    not null,
    condition_json            json                                                   not null,
    created_at                datetime(6)                                            not null,
    duration_days             int                                                    not null,
    end_date                  date                                                   null,
    keep_phase                bit                                                    not null,
    name                      varchar(255)                                           null,
    progress                  decimal(38, 2)                                         null,
    reason                    text                                                   null,
    redo                      bit                                                    not null,
    start_date                date                                                   null,
    status                    enum ('COMPLETED', 'CREATED', 'FAILED', 'IN_PROGRESS') null,
    total_missions            int                                                    not null,
    quit_plan_id              int                                                    null,
    system_phase_condition_id int                                                    null,
    constraint FK82bytt8ef5ythnj7lfvk1ucly
        foreign key (system_phase_condition_id) references system_phase_condition (id),
    constraint FKjyny2nrdf476q1kgfk8f0vnv1
        foreign key (quit_plan_id) references quit_plan (id)
);

create table phase_detail
(
    id        int auto_increment
        primary key,
    date      date         null,
    day_index int          not null,
    name      varchar(255) null,
    phase_id  int          null,
    constraint FKfkxlxvljb16x35bmrnmgp1qob
        foreign key (phase_id) references phase (id)
);

create table phase_detail_mission
(
    id              int auto_increment
        primary key,
    code            varchar(255)                                null,
    completed_at    datetime(6)                                 null,
    description     varchar(255)                                null,
    name            varchar(255)                                null,
    status          enum ('COMPLETED', 'FAILED', 'INCOMPLETED') null,
    mission_id      int                                         null,
    phase_detail_id int                                         null,
    constraint FKbwtivpk1lcgqhggtd0k33my0h
        foreign key (mission_id) references mission (id),
    constraint FKoqk14ngxnv9ug6yejo5fq7fsk
        foreign key (phase_detail_id) references phase_detail (id)
);

create table reminder_queue
(
    id                   int auto_increment
        primary key,
    content              text                                  null,
    scheduled_at         datetime(6)                           null,
    status               enum ('CANCELLED', 'PENDING', 'SENT') null,
    phase_detail_id      int                                   null,
    reminder_template_id int                                   null,
    constraint FK304ve5k1hy1dmxs76iwq89n75
        foreign key (phase_detail_id) references phase_detail (id),
    constraint FKsithgfcmxdjuf9s5280gd1cn8
        foreign key (reminder_template_id) references reminder_template (id)
);


