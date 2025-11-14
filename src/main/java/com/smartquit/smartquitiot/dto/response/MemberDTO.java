package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartquit.smartquitiot.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberDTO {

    int id;
    String email;
    String firstName;
    String lastName;
    String avatarUrl;
    Gender gender;
    LocalDate dob;
    Integer age;
    boolean isUsedFreeTrial;
    AccountDTO account;
    LocalTime morningReminderTime;
    LocalTime quietStart;
    LocalTime quietEnd;
    String timeZone;
}
