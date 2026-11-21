# Java Personal Project <hr>
## Subject
Problem : 학년이 올라갈수록 기존의 것과 더불어 파일 관리가 힘들어짐.       
또 지금 학원 조교일을 하는데 하루에 다운받는 파일만 수십 개에 관리도 잘 안 돼서 같은 파일 여러 개 다운받고 찾기도 힘듦-> 작업 효율 떡락   
찾고자 하는 파일들을 못 찾으면 화가 남, 자주 쓰는데 정리 안 된 거 보면 짜증남.  

Selection Criteria : 
1. 과한 모듈 사용 X
2. 다양한 문법 사용
3. 난이도 너무 높으면 X
4. 구현 과정이 직관적이었음 좋겠다   

Solution : 다운로드 파일의 주 경로인 Download 폴더를 감시, 이하는 Function Requirements의 순서대로
## Requirements
### Function Requirements
* 폴더 감시 : 지정한 폴더에 새 파일이 생기면 감지
* 규칙 매칭 : 확장자, 파일명 키워드(정규식)로 어떤 규칙에 해당하는지 판단
* 이름 변경 : 규칙에 정의된 패턴대로 파일명 변경
* 파일 이동 : 규칙에 지정된 대상 폴더로 이동, 폴더가 없으면 생성
* 충돌 처리 : 같은 이름 파일이 있으면 (1), (2) 붙이기
* 기존 파일 일괄 정리 : 실행 시 폴더에 이미 있는 파일도 한 번에 정리
* 미리보기 : 실제로 옮기지 않고 '무엇이 어디로 갈지'만 출력
* 로그와 되돌리기 : 처리 내역을 기록하고. 마지막 작업을 원상복구
### Non-Functional Requirements
* 규칙은 코드 수정 없이 설정 파일(Json)로 추가/변경 가능
* 다운로드 중인 임시 파일은 무시
* 오류가 나도 프로그램 전체가 죽지 않고 해당 파일만 건너뛰기

## Structure

| 경로 | 클래스/모듈 | 설명 |
|---|---|---|
| `app/` | `Main` | 실행 진입점, 모드 선택(감시/일괄/미리보기/되돌리기) |
| `config/` | `ConfigLoader` | JSON을 `Rule` 객체로 변환 |
|  | `Rule`, `MatchCondition` | 규칙 데이터 |
| `core/` | `FolderWatcher` | `WatchService`로 이벤트 수신 |
|  | `RuleMatcher` | 파일에 맞는 규칙 찾기 |
|  | `NameResolver` | `{1}`, `{ext}`, `{yyyy-MM}` 같은 패턴 치환 |
|  | `FileMover` | 파일 이동 및 충돌 처리 |
|  | `FileProcessor` | 위 기능을 묶어 파일 하나를 처리 |
| `history/` | `HistoryLog` | 처리 내역 저장 |
|  | `UndoManager` | 되돌리기 |
| `util/` | `TempFileFilter` | 임시 파일 판별 |   

## 구현 단계
1. 하드코딩한 규칙 하나로 폴더 내 기존 파일을 확장자별 이동 | Files.list, Files.move
2. 충돌 처리, 임시 파일 무시, 예외 처리 | 예외 설계, Path 다루기   
-> 여기까지만 해도 작동. 검증후 이후 단계로 살 붙여나가기. **1차 검증**
3. 규칙을 Json으로 분리 | 객체 매핑
4. 정규식 매칭과 이름 패턴 치환 | Pattern, Mathcer 그룹
5. 실시간 감시 모드 | WatchService, 스레드
6. 미리보기 모드, 로그, 되돌리기 | 설계 확장, 파일 기록
7. 트레이 아이콘이나 간단한 GUI, JUunit 테스트 | Swing/JavaFX, 테스트  

### 구성요소 설계
0단계. 프로젝트 준비
프로젝트 생성 (IntelliJ 기준 New Project → Java)
패키지 폴더 만들기: app, config, core, history, util


1단계. 기존 파일을 일련의 규칙대로 옮기기
Main — 감시 폴더 경로를 하드코딩하고 Files.list로 파일 목록 출력
FileMover — 파일 하나를 대상 폴더로 이동 
Main에서 와일드카드를 사용하여 규칙에 맞는 파일 감지, 대상 폴더로 이동

2단계. 안전장치
TempFileFilter — .crdownload, .part, .tmp 판별
FileMover에 충돌 처리 추가 — 같은 이름이면 (1), (2) 붙이기
파일 하나 실패해도 계속 진행되게 try-catch 정리

3단계. 규칙을 객체와 JSON으로 분리
MatchCondition, Rule — 데이터 클래스
RuleMatcher — 파일에 맞는 Rule 찾기 (일단 확장자만)
ConfigLoader — config.json 읽어서 List<Rule>로 변환 (Gson)
FileProcessor — "매칭 → 이동" 흐름 묶기
Main의 if문 하드코딩을 걷어내고 FileProcessor 호출로 교체

4단계. 정규식과 이름 바꾸기
RuleMatcher에 namePattern 매칭 추가
NameResolver — {1}, {ext}, {name}, {yyyy-MM} 치환
FileProcessor에 이름 결정 단계 끼워 넣기

✅ 확인: lecture_03.pdf → 데이터통신_03주차.pdf로 바뀌어 이동

5단계. 실시간 감시
FolderWatcher — WatchService로 새 파일 이벤트 수신
파일 크기가 안정될 때까지 기다렸다가 처리 (ScheduledExecutorService)
중복 이벤트 걸러내기
Main에 모드 선택 추가: 일괄 정리 / 감시

✅ 확인: 프로그램 켜둔 상태에서 파일을 넣으면 자동으로 옮겨짐

6단계. 미리보기, 로그, 되돌리기
FileProcessor에 dry-run 옵션 — 이동 대신 출력만
HistoryLog — "원래 위치 → 새 위치"를 파일에 기록
UndoManager — 마지막 실행분을 역순으로 되돌리기

✅ 확인: 정리 후 되돌리기 하면 원래대로 돌아옴

7단계 (선택). 테스트와 UI
NameResolver, RuleMatcher부터 JUnit 테스트 작성
여유 있으면 트레이 아이콘이나 간단한 Swing 창

### 주의
* 다운로드 직후 파일이 잠겨 있음 : 갖미자하자마자 옮기면 실패할 수 있어서, 파일 크기가 일정 시간 변하지 않을 때까지 잠깐 기다리는 로직 필요
* 이벤트 중복 : WatchService는 한 파일에 생성/수정 이벤트를 여러 번 보낼 수 있음
* 자기 자신 감시 : 대상 폴더가 감시 폴더 안에 있으면 옮긴 파일을 다시 감지할 수 있으니 하위 폴더는 감시에서 제외 필요
### 피드백
* 구현 과정은 어렵지 않을 것으로 예상, 근데 실험 단계에서 애로사항이 있을 거임
* 프로그램 실행시 일련의 작업을 할 것인지, 실행 안 해도 자동으로 감지해서 시행할 것인지.