# Лаборатори №8: Mock объектууд — Mockito

О.Дэлгэрмаа B232270024

## Орчин

`java -version`:

```
delgermaa@debian:~/lab08$ java -version
openjdk version "17.0.19" 2026-04-21
OpenJDK Runtime Environment (build 17.0.19+10-1-deb12u2-Debian)
OpenJDK 64-Bit Server VM (build 17.0.19+10-1-deb12u2-Debian, mixed mode, sharing)
```

`mvn -version`:

```
delgermaa@debian:~/lab08$ mvn -version
Apache Maven 3.9.9
Maven home: /usr/share/maven
Java version: 17.0.19, vendor: Debian, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "6.12.111+deb13-amd64", arch: "amd64", family: "unix"
```

## Төслийн бүтэц

```
lab08-mockito/
├── pom.xml
├── src/main/java/mn/edu/must/sqat/      # starter (өөрчлөөгүй)
├── src/test/java/mn/edu/must/sqat/
│   └── CourseRegistrationServiceTest.java
├── results/
│   ├── mvn-test.txt
│   ├── mvn-test-mutant-1.txt
│   ├── mvn-test-mutant-2.txt
│   └── mvn-test-mutant-3.txt
└── README.md
```

## Хэсэг A: Тестүүд

`CourseRegistrationService`-ийн хоёр хамаарал (`StudentRepository`, `EmailSender`)-ийг `@Mock`-оор орлуулж, 8 тест бичсэн.

| № | Тест | Ашигласан хэрэгсэл |
|---|---|---|
| A1 | `register_activeStudent_returnsOkSavesAndSendsEmail` | `when` + `verify` |
| A2 | `register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail` | `verify(..., never())` |
| A3 | `register_inactiveStudent_returnsInactiveAndNoEmailInteraction` | `verifyNoInteractions` |
| A4 | `register_missingPrerequisites_returnsPrerequisitesErrorAndDoesNotSave` | `when` + `never` |
| A5 | `register_emailSenderThrows_stillReturnsOkAndSaves` | `doThrow(...).when(...)` |
| A6 | `register_nullArguments_throwsIllegalArgumentAndTouchesNothing` | `assertThrows` |
| A7 | `register_success_emailBodyContainsCourseCode` | `ArgumentCaptor` |
| A8 | `register_errorScenarios_returnExpectedErrorCode` | `@ParameterizedTest` + `@CsvSource` |

## Тестийн тоо

`results/mvn-test.txt`-ийн сүүлийн мөр: **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0**, **BUILD SUCCESS**.

10 = 7 энгийн тест (A1–A7) + A8-ийн 3 мөр (мөр бүр тусдаа тест болж тоологдоно).

## A8 дахь `lenient()`-ийн тайлбар

A8-д мөр бүр нь `findStudent`, `courseExists`, `prerequisites` гурвыг бүгдийг нь stub хийдэг. Гэтэл «оюутан байхгүй» эсвэл «идэвхгүй» мөрөнд `register()` эхний шалгалтаар эрт буцдаг тул сүүлийн stub-үүд огт дуудагдахгүй. MockitoExtension-ий strict stubs горим ийм хэрэглэгдээгүй stub-ийг `UnnecessaryStubbingException` гэж алдаа болгодог тул `lenient().when(...)` ашигласан.

## Хэсэг B: Мини мутаци

Мутант бүрийн дараа кодыг `git checkout -- src/main`-аар сэргээсэн.

| Мутант | Эвдрэл | Унасан тестүүд | Failures / Errors | Үр дүн |
|---|---|---|---|---|
| 1 | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";` мөрийг комментлосон | `register_inactiveStudent_returnsInactiveAndNoEmailInteraction` (A3), `register_errorScenarios_returnExpectedErrorCode[2]` (A8, идэвхгүй оюутны мөр) | Failures: 2, Errors: 0 | BUILD FAILURE |
| 2 | `repository.saveRegistration(studentId, courseId);` дуудалтыг комментлосон | `register_activeStudent_returnsOkSavesAndSendsEmail` (A1), `register_emailSenderThrows_stillReturnsOkAndSaves` (A5) | Failures: 2, Errors: 0 | BUILD FAILURE |
| 3 | эхний `if`-ээс `|| courseId == null`-ийг устгасан | `register_nullArguments_throwsIllegalArgumentAndTouchesNothing` (A6) | Failures: 1, Errors: 0 | BUILD FAILURE |

Дэлгэрэнгүй: `results/mvn-test-mutant-1.txt`, `results/mvn-test-mutant-2.txt`, `results/mvn-test-mutant-3.txt`.

**Дүгнэлт (Хэсэг B):**

- Мутант 1: идэвхгүй оюутны шалгалтыг авбал A3 нь `ERROR_INACTIVE_STUDENT`-ийн оронд `ERROR_NO_COURSE` авсан (`courseExists` stub байхгүй тул `false` буцсан). A8-ийн 2-р мөр нь `OK` авсан. Хоёулаа мутантыг барьсан.
- Мутант 2: `saveRegistration` дуудагдахгүй болоход `verify(repository).saveRegistration(...)` хийдэг A1 ба A5 «Wanted but not invoked» гэж унасан.
- Мутант 3: A6 нь `register(null, "CS201")` болон `register("S1", null)` хоёр аргумент тус бүрийн `null`-ийг тусад нь шалгадаг тул `courseId == null` шалгалтыг устгахад `register("S1", null)` нь exception шидэхгүй `ERROR_NO_STUDENT` буцааж, тест унасан. Мутант шууд баригдсан тул тестэд нэмэлт засвар хийх шаардлагагүй болсон.
- Гурван мутант бүгд баригдсан, ямар ч эвдрэл тестийн хамгаалалтаас гараагүй.

## Дүгнэлт

Mock нь хамаарлыг орлуулж, түүнтэй хийсэн харилцааг ямар метод, ямар аргументтай, хэдэн удаа дуудагдсан шалгах боломж өгдөг. Харин гараар бичсэн fake нь жинхэнэ хэрэгжүүлэлтийг хялбарчилсан хувилбар (жишээ нь санах ойн жагсаалтад хадгалдаг repository) бөгөөд төлөв дээр тулгуурлан үр дүнг шалгадаг. Энэ лабд mock нь «хадгалалт дуудагдсан уу, и-мэйл явсан уу» гэдгийг шууд verify хийх боломж өгсөн бол fake-ийг бичиж, засварлах нэмэлт ажил гарах байсан. Харин mock-ийг дутуу тохируулбал  тест санамсаргүй зөв/буруу болох эрсдэлтэй. Өгөгдлийн объект (`Student` record), `List` зэрэг энгийн утгын төрлүүдийг mock хийх ёсгүй, мөн тестлэж буй классаа өөрийг нь mock хийж болохгүй, учир нь тэгвэл бодит логик тестлэгдэхгүй. Mock хийх зүйл нь удаан, найдваргүй эсвэл гадаад хамаарал (өгөгдлийн сан, SMTP сервер, сүлжээ) байх ёстой. `verify`-ийг хэтрүүлбэл тест кодын дотоод хэрэгжүүлэлтэд хэт уягдаж, үр дүн нь өөрчлөгдөөгүй мөртлөө зүгээр л дуудалтын дараалал эсвэл тоо өөрчлөгдөхөд тест унадаг. Ийм тест refactor хийхэд саад болж, засвар хийх зардлыг нэмдэг. Иймд зөвхөн бизнесийн ач холбогдолтой харилцааг  verify хийж, бусдыг нь үр дүнгээр шалгах нь зөв.
Mock нь хамаарлыг орлуулж, түүнтэй хийсэн харилцааг ямар метод, ямар аргументтай, хэдэн удаа дуудагдсан шалгах боломж өгдөг. Харин гараар бичсэн fake нь жинхэнэ хэрэгжүүлэлтийг хялбарчилсан хувилбар (жишээ нь санах ойн жагсаалтад хадгалдаг repository) бөгөөд төлөв дээр тулгуурлан үр дүнг шалгадаг. Энэ лабд mock нь «хадгалалт дуудагдсан уу, и-мэйл явсан уу» гэдгийг шууд verify хийх боломж өгсөн бол fake-ийг бичиж, засварлах нэмэлт ажил гарах байсан. Харин mock-ийг дутуу тохируулбал  тест санамсаргүй зөв/буруу болох эрсдэлтэй. Өгөгдлийн объект (`Student` record), `List` зэрэг энгийн утгын төрлүүдийг mock хийх ёсгүй, мөн тестлэж буй классаа өөрийг нь mock хийж болохгүй, учир нь тэгвэл бодит логик тестлэгдэхгүй. Mock хийх зүйл нь удаан, найдваргүй эсвэл гадаад хамаарал (өгөгдлийн сан, SMTP сервер, сүлжээ) байх ёстой. `verify`-ийг хэтрүүлбэл тест кодын дотоод хэрэгжүүлэлтэд хэт уягдаж, үр дүн нь өөрчлөгдөөгүй мөртлөө зүгээр л дуудалтын дараалал эсвэл тоо өөрчлөгдөхөд тест унадаг. Ийм тест refactor хийхэд саад болж, засвар хийх зардлыг нэмдэг. Иймд зөвхөн бизнесийн ач холбогдолтой харилцааг  verify хийж, бусдыг нь үр дүнгээр шалгах нь зөв.
