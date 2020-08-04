package syudy.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import syudy.querydsl.dto.MemberDto;
import syudy.querydsl.dto.QMemberDto;
import syudy.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.filter;
import static org.assertj.core.api.AssertionsForClassTypes.setMaxLengthForSingleLineDescription;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Slf4j
class MemberTest {

    @Autowired
    private EntityManager em;

    private JPAQueryFactory query;

    @BeforeEach
    void setUp() {
        final Team teamA = new Team("Team-A");
        final Team teamB = new Team("Team-B");
        em.persist(teamA);
        em.persist(teamB);

        final Member member1 = new Member("DOBY", 31, teamA);
        final Member member2 = new Member("RED", 33, teamA);
        final Member member3 = new Member("LIME", 28, teamB);
        final Member member4 = new Member("NORI", 30, teamB);
        final Member member5 = new Member(null, 28, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        em.persist(member5);

        em.flush();
        em.clear();

        this.query = new JPAQueryFactory(em);
    }

    @Test
    @DisplayName("Member 객체 삽입 후 조회")
    void test_select_01() {
       final List<Member> members = em.createQuery("select m from Member m", Member.class)
                                      .getResultList();

       assertEquals(5, members.size());
    }

    @Test
    @DisplayName("Member 객체 조회 where 조건문")
    void test_select_02_where_01() {
        final QMember member = QMember.member;
        final Member findMember = query.select(member).from(member)
                                       .where(member.username.like("%DO%").and(member.age.gt(20)))
                                       .fetchOne();

        /*
            gt  >
            goe >=
            lt  <
            loe <=
        */

        assertNotNull(findMember);
        assertEquals(31, findMember.getAge());
    }

    @Test
    @DisplayName("Member 객체 조회 where 조건문")
    void test_select_02_where_02() {
        final QMember member = QMember.member;
        /* 같은 and 조건에 대해서는 , (comma) 활용 가능 */
        final Member findMember = query.select(member).from(member)
                .where(member.username.like("%DO%"), (member.age.gt(20)))
                .fetchOne();

        assertNotNull(findMember);
        assertEquals(31, findMember.getAge());
    }

    @Test
    @DisplayName("Member 객체 조회 fetch")
    void test_select_03_fetch_01() {
        final QMember member = QMember.member;
        /*
            fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
            fetchOne() : 단 건 조회
                결과가 없으면 : null,
                결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException

            fetchFirst() : limit(1).fetchOne()
            fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
            fetchCount() : count 쿼리로 변경해서 count 수 조회
        */

        final List<Member> fetch = query.selectFrom(member).fetch();
        assertEquals(5, fetch.size());

        final Member fetchOne = query.selectFrom(member)
                                     .where(member.username.eq("BRAD"))
                                     .fetchOne();
        assertNull(fetchOne);

        final Member fetchFirst = query.select(member).from(member).fetchFirst();
        assertNotNull(fetchFirst);

        /* PAGING에서 사용하는 조회 방식, totalCount + select 두개의 쿼리가 실제로 동작 */
        final QueryResults<Member> results = query.selectFrom(member).fetchResults();
        assertEquals(5, results.getTotal());
        assertEquals(5, results.getResults().size());

        final long fetchCount = query.selectFrom(member).fetchCount();
        assertEquals(5, fetchCount);
    }

    @Test
    @DisplayName("Member 객체 조회 fetch, fetchOne를 시도했을 경우, 데이터가 1개 초과이면 예외발생")
    void test_select_03_fetch_02() {
        final QMember member = QMember.member;

        assertThrows(NonUniqueResultException.class, () ->{
            query.selectFrom(member).fetchOne();
        });
    }

    @Test
    @DisplayName("Member 객체 조회 정렬")
    void test_select_04_sort_01() {
        final QMember member = QMember.member;

        /* 나이 많은 순서, 이름 가나다 순서이고, 이름이 NULL이면 마지막 */
        final List<Member> members = query.selectFrom(member)
                                          .orderBy(member.age.desc(), member.username.asc().nullsLast())
                                          .fetch();
        assertEquals("RED", members.get(0).getUsername());
        assertEquals("LIME", members.get(3).getUsername());
        assertNull(members.get(4).getUsername());
    }

    @Test
    @DisplayName("Member 객체 조회 페이")
    void test_select_05_page_01() {
        final QMember member = QMember.member;
        final QueryResults<Member> memberResults = query.selectFrom(member)
                                                        .orderBy(member.age.desc().nullsFirst())
                                                        .offset(2)
                                                        .limit(2)
                                                        .fetchResults();
        final List<Member> members = memberResults.getResults();
        assertEquals(5, memberResults.getTotal());
        assertEquals(2, members.size());
        assertEquals("NORI", members.get(0).getUsername());
        assertNull(members.get(1).getUsername());
    }

    @Test
    @DisplayName("Member 객체 조회 집합")
    void test_select_06_aggregation_01() {
        final QMember member = QMember.member;
        final Tuple tuple = query.select(
                                    member.count(),
                                    member.age.sum(),
                                    member.age.avg(),
                                    member.age.max(),
                                    member.age.min())
                                .from(member)
                                .fetchOne();

        assertEquals(5, tuple.get(member.count()));
        assertEquals(28, tuple.get(member.age.min()));
        assertEquals(33, tuple.get(member.age.max()));
    }

    @Test
    @DisplayName("Member 객체 조회 Group by, 팀의 이름과 각 팀의 평균 연령 구하기")
    void test_select_06_aggregation_02() {
        final QMember member = QMember.member;
        final QTeam team = QTeam.team;
        final List<Tuple> tuples = query.select(team.name, member.age.avg())
                                        .from(team)
                                        .join(member)
                                        .on(team.eq(member.team))
                                        .groupBy(team.name)
                                        .fetch();

        assertEquals(32, tuples.get(0).get(member.age.avg()));
    }

    @Test
    @DisplayName("Member, Team 조인 조회, Team A 소속된 모든 팀원 출력 (연관관계 O)")
    void test_select_07_join_01() {
        final QMember member = QMember.member;
        final QTeam team = QTeam.team;
        /* Querydsl은 JPQL의 Builder 이다! */
        final List<Member> members = query.select(member)
                                          .from(member)
                                          .join(member.team, team)
                                          .where(member.team.name.eq("Team-A"))
                                          .fetch();
        assertEquals(2, members.size());
        assertThat(members).extracting("username").containsExactly("DOBY", "RED");
    }

    @Test
    @DisplayName("Member, Team 조인 조회, 팀원 전원 출력 ")
    void test_select_08_join_01() {
        /*
            ON 의 역할
                - 연관관계가 없는 엔티티간의 조인 (제일 많이 사용)
                - 조인대상 필터링
            join == inner join : 대상이 되는 값만 출력
                - on 으로 조건을 설정하는 것과 where 조건을 설정하는 것이 실제로 동일하게 동작
            left join : 왼쪽 기준으로 출력
                - 예를들어, member join team on member.team.name.eq("A") 이면 모든 회원을 출력하지만, A가 아닌 사람은 null 출력
        */
        final QMember member = QMember.member;
        final QTeam team = QTeam.team;
        /* Querydsl은 JPQL의 Builder 이다! */
        final List<Tuple> members = query.select(member, team)
                                         .from(member)
                                         .leftJoin(member.team, team)
                                         .on(member.team.name.eq("Team-A"))
                                         .fetch();
        assertEquals(5, members.size());
        for (Tuple tuple : members) {
            log.info("member : {}", tuple.toString());
        }
    }

    @Test
    @DisplayName("연관관계가 없는 엔티티간의 조인, 회원의 이름과 팀 이름이 같은 대상 출력 ")
    void test_select_08_join_02() {
        em.persist(new Member("Team-A"));
        em.persist(new Member("Team-B"));
        em.persist(new Member("Team-C"));

        final QMember member = QMember.member;
        final QTeam team = QTeam.team;
        /* 연관관계가 있고 없고는, join() 에서의 문법이 다르다! */
        final List<Tuple> members = query.select(member, team)
                                         .from(member)
                                         .innerJoin(team)
                                         .on(member.username.eq(team.name))
                                         .fetch();
        assertEquals(2, members.size());
        for (Tuple tuple : members) {
            log.info("member : {}", tuple.toString());
        }
    }

    @Autowired
    private EntityManagerFactory emf;

    @Test
    @DisplayName("지연로딩이지만 Member,Team SQL 한번에 조회")
    void test_select_09_fetchjoin_01() {
        /* fetch join은 SQL 조인을 활용해서 엔티티를 한번에 조회하는 기능. 주로 최적화에서 많이 사용되는 기능 */

        final QMember member = QMember.member;
        final QTeam team = QTeam.team;

        final Member findMember = query.selectFrom(member)
                                       .where(member.username.eq("DOBY"))
                                       .fetchOne();
        /* 지연로딩이자만 하나의 SQL에서 전체 다 조회 */
        log.info("Member : {}", findMember.toString());

        /* 로딩유무를 확인 (getTeam) */
        final boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertFalse(loaded);
    }

    @Test
    @DisplayName("지연로딩으로 Member,Team SQL 각각 실행")
    void test_select_09_fetchjoin_02() {
        /* fetch join은 SQL 조인을 활용해서 엔티티를 한번에 조회하는 기능. 주로 최적화에서 많이 사용되는 기능 */

        final QMember member = QMember.member;
        final QTeam team = QTeam.team;
        final Member findMember = query.selectFrom(member)
                                       .join(member.team, team)
                                       .fetchJoin()
                                       .where(member.username.eq("DOBY"))
                                       .fetchFirst();

        final boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertTrue(loaded);
    }

    @Test
    @DisplayName("회원의 나이를 20대, 30대로 구분")
    void test_select_10_case_01() {
        final QMember member = QMember.member;
        final String memAge = query.select(member.age
                .when(20).then("20대")
                .when(30).then("30대")
                .otherwise("노인")

        ).from(member)
                .orderBy(member.age.desc())
                .fetchFirst();
        assertEquals("30대", memAge);
    }

    @Test
    @DisplayName("회원의 나이를 20대, 30대로 구분")
    void test_select_10_case_02() {
        /* case 를 사용해서 복잡한 쿼리를 하기보다는, 단지 데이터를 조회하고, domain 에서 처리하는 것이 좋다고 생각! */
        final QMember member = QMember.member;
        final String memAge = query.select(
                                        new CaseBuilder().when(member.age.between(20, 30)).then("20대")
                                                         .otherwise("30대")
                                    ).from(member)
                                   .orderBy(member.age.desc())
                                   .fetchFirst();
        assertEquals("30대", memAge);
    }

    @Test
    @DisplayName("상수 출력")
    void test_select_11_constant_01() {
        final QMember member = QMember.member;
        final Tuple tuple = query.select(member.username, Expressions.constant("A"))
                .from(member)
                .where(member.username.eq("DOBY"))
                .fetchFirst();
        log.info("Tuple : {}", tuple.toString());
    }

    @Test
    @DisplayName("문자열 붙이기")
    void test_select_12_conncat_01() {
        final QMember member = QMember.member;
        /* age는 정수형이기 때문에 문자열 처리가 필요, 그리고 Enum을 사용할때 많이 사용 */
        final String result = query.select(member.username.concat("_").concat(member.age.stringValue()))
                                   .from(member)
                                   .where(member.username.eq("DOBY"))
                                   .fetchFirst();
        assertEquals("DOBY_31", result);
    }

    @Test
    @DisplayName("나이가 가장 많은 회원을 조회")
    void test_select_13_subquery_01() {
        final QMember member = QMember.member;
        final QMember memberSub = QMember.member;

        final Member findMember = query.selectFrom(member)
                                       .where(member.age.eq(
                                               JPAExpressions
                                                       .select(memberSub.age.max())
                                                       .from(memberSub)
                                       )).fetchOne();
        assertEquals(findMember.getAge(), 33);
    }

    @Test
    @DisplayName("나이가 평균 이상인 회원 조회")
    void test_select_13_subquery_02() {
        final QMember member = QMember.member;
        final QMember memberSub = QMember.member;

        final List<Member> members = query.selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();
        assertEquals(3, members.size());
    }

    @Test
    @DisplayName("도비와 회원들 최대 나이 출력")
    void test_select_13_subquery_03() {
        final QMember member = QMember.member;
        final QMember memberSub = QMember.member;

        final Tuple tuple = query.select(member.username, JPAExpressions.select(memberSub.age.max()).from(memberSub))
                                 .from(member)
                                 .where(member.username.eq("DOBY"))
                                 .fetchFirst();
        assertEquals(33, tuple.get(1, Integer.class));
        assertEquals("DOBY", tuple.get(0, String.class));

        /*
            JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
            당연히 Querydsl도 지원하지 않는다. 왜냐하면, Querydsl은 JPQL 빌더이기 때문이다.
            하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다.
            Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.

            from 절의 서브쿼리 해결방안
                1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
                2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
                3. nativeSQL을 사용한다.

            from 절 내부에 서브쿼리가 과연 단지 데이터를 조회하는 것이 아닌, 비즈니스 로직이 있지 않을까? 라는 의문을 한번씩 가져보자!
        */
    }

    @Test
    @DisplayName("DTO를 이용한 데이터 조회 - setter 방식")
    void test_select_14_projection_01() {
        /*
            Tuple은 Querydsl 에서 제공하는 클래스이기 때문에 repository, domain layer 까지만 사용하는 것을 권장
            또한, Querydsl이 아닌, 다른 방식으로 변경을 했을 경우에도 편리함
            그렇기 때문에 Tuple 사용하기보다는 DTO를 활용 권장
        */
        /*
            NoArgsConstructor + setter 필요 (getter 불필요), 반드시 엔티티 필드명과 DTO 필드명이 동일해야 setter로 넣을 수 있음
        */
        final QMember member = QMember.member;
        final MemberDto findMember = query
                                        .select(Projections.bean(MemberDto.class, member.username, member.age))
                                        .from(member)
                                        .where(member.username.eq("DOBY"))
                                        .fetchOne();
        assertEquals(31, findMember.getAge());
    }

    @Test
    @DisplayName("DTO를 이용한 데이터 조회 - 필드접근 방식")
    void test_select_14_projection_02() {
        /*
            Getter, Setter 불필요, 필드에 넣는 방식, 반드시 엔티티 필드명과 DTO 필드명이 동일해야 field에 넣을 수 있음
        */
        final QMember member = QMember.member;
        final MemberDto findMember = query
                                        .select(Projections.fields(MemberDto.class, member.username, member.age))
                                        .from(member)
                                        .where(member.username.eq("DOBY"))
                                        .fetchOne();
        assertEquals(31, findMember.getAge());
    }

    @Test
    @DisplayName("DTO를 이용한 데이터 조회 - 생성자 방식")
    void test_select_14_projection_03() {
        final QMember member = QMember.member;
        /*
            field이 타입이 동일해야 가능 나이가 "11" 이면 안됨!
            타입이 맞지 않으면 intellij에서 오류 발생 (컴파일)
        */
        final MemberDto findMember = query
                                        .select(Projections.constructor(MemberDto.class, member.username, member.age))
                                        .from(member)
                                        .where(member.username.eq("DOBY"))
                                        .fetchOne();
        assertEquals(31, findMember.getAge());
    }

    @Test
    @DisplayName("DTO를 이용한 데이터 조회 - alias - 필드")
    void test_select_14_projection_04() {
        final QMember member = QMember.member;
        /*
            DTO와 Entity의 필드명이 다를 경우 alias 부여
        */
        final UserDto findUser = query
                                    .select(Projections.fields(UserDto.class, member.username.as("name"), member.age))
                                    .from(member)
                                    .where(member.username.eq("DOBY"))
                                    .fetchOne();
        assertEquals(31, findUser.getAge());
        assertEquals("DOBY", findUser.getName());
    }

    @Test
    @DisplayName("DTO를 이용한 데이터 조회 - alias - 서브쿼리")
    void test_select_14_projection_05() {
        final QMember member = QMember.member;
        final QMember memberForSub = new QMember("sub");
        /*
            DTO와 Entity의 필드명이 다를 경우 alias 부여
        */
        final List<UserDto> users = query
                                        .select(Projections.fields(UserDto.class, member.username.as("name"),
                                                ExpressionUtils.as(JPAExpressions
                                                                    .select(memberForSub.age)
                                                                    .from(memberForSub)
                                                                    .where(member.username.eq(memberForSub.username)), "age")))
                                        .from(member)
                                        .fetch();
        for (UserDto user : users) {
            log.info(" --> User : {}", user.toString());
        }
    }

    @Test
    @DisplayName("@QueryProjection를 이용한 데이터 조회")
    void test_select_14_projection_06() {
        final QMember member = QMember.member;

        /*
            QueryProejction을 사용하면 DTO도 QClass를 생성 (생성자 타입과 유사하게 타입이 중요)
                - 장점 : QueryProjection은 생성자가 실제로 존재하기 때문에 생성자 타입과 달리 컴파일로 확인 가능
                - 단점 :
                    - 필드가 많을 경우에는 불편함
                    - DTO를 QClass로 만들어야 하는 불편함
                    - DTO가 Querydsl에 대한 의존성이 발생
                        - 갑자기 Querydsl을 사용하지 않을 경우, DTO도 수정해야하는 의존성에 대한 문제
                        - DTO가 여러 계층에서 사용할 수 있는데, Querydsl과 혼종이 되어서 순수한 DTO라고 할 수 없음
        */

        final List<MemberDto> members = query
                                            .select(new QMemberDto(member.username, member.age))
                                            .from(member)
                                            .fetch();
        for (MemberDto memberDto : members) {
            log.info(" --> MemberDto : {}", memberDto.toString());
        }
    }

    @Test
    @DisplayName("BooleanBuilder를 이용한 where 조건 조회")
    void test_select_15_booleanBuilder_01() {
        final QMember member = QMember.member;
        final String searchName = "";
        final Integer searchAge = 31;

        final BooleanBuilder builder = new BooleanBuilder();
        if(!searchName.equals("")) {
            builder.and(member.username.eq(searchName));
        }
        if(searchAge != null) {
            builder.and(member.age.eq(searchAge));
        }

        final MemberDto memberDto  = query
                                        .select(new QMemberDto(member.username, member.age))
                                        .from(member)
                                        .where(builder)
                                        .fetchFirst();
        assertEquals("DOBY", memberDto.getUsername());
        assertEquals(31, memberDto.getAge());
    }

    @Test
    @DisplayName("Where 다중 파라미터를 이용한 조회")
    void test_select_16_multiparam_01() {
        final QMember member = QMember.member;
        final String searchName = "";
        final Integer searchAge = 31;

        /*
            장점
                1. 쿼리가 간결
                2. 메소드를 통해서 해당 조건이 어떤 역핧을 수행하는지 알 수 있음 (직관적)
        */

        final MemberDto memberDto  = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .where(
                        /* where 에 null 이 있으면 null 은 무시 */
                        usernameEq(member, searchName), ageEq(member, searchAge)
                )
                .fetchFirst();
        assertEquals("DOBY", memberDto.getUsername());
        assertEquals(31, memberDto.getAge());
    }

    @Test
    @DisplayName("Where 다중 파라미터를 이용한 조회")
    void test_select_16_multiparam_02() {
        final QMember member = QMember.member;
        final String searchName = "";
        final Integer searchAge = 31;

        /*
            장점
                1. 쿼리가 간결
                2. 메소드를 통해서 해당 조건이 어떤 역핧을 수행하는지 알 수 있음 (직관적)
                3. 조립이 가능
                4. 재사용의 기대성이 좋음
        */

        final MemberDto memberDto  = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .where(
                        /* where 에 null 이 있으면 null 은 무시 */
                        allEq(member, searchName, searchAge)
                )
                .fetchFirst();
        assertEquals("DOBY", memberDto.getUsername());
        assertEquals(31, memberDto.getAge());
    }

    private Predicate ageEq(QMember member, Integer searchAge) {
        return searchAge == null ? null : member.age.eq(searchAge);
    }

    private Predicate usernameEq(QMember member, String searchName) {
        return searchName == null ? null : member.username.endsWith(searchName);
    }

    private BooleanExpression ageEqByExpression(QMember member, Integer searchAge) {
        return searchAge == null ? null : member.age.eq(searchAge);
    }

    private BooleanExpression nameEqByExpression(QMember member, String searchName) {
        return searchName == null ? null : member.username.endsWith(searchName);
    }

    private BooleanExpression allEq(QMember member, String searchName, Integer searchAge) {
        return nameEqByExpression(member, searchName).and(ageEqByExpression(member, searchAge));
    }


    @Test
    @DisplayName("BULK Delete")
    void test_dml_01_bulk_delete_01() {
        final QMember member = QMember.member;
        long count = query.delete(member)
                          .where(member.age.goe(31))
                          .execute();
        final List<Member> members = query.selectFrom(member).fetch();
        assertEquals(3, members.size());
    }

    @Test
    @Commit
    @DisplayName("BULK Update")
    void test_dml_01_bulk_update_02() {
        /*
            JPA는 변경감지를 통해 변경된 부분에 대해 데이터 변경 --> 건건이 데이터를 삽입
                --> 문제 : BULK가 아니라.. DB 호출 건수가 너무 많음 --> Bulk 연산 필요

            Bulk 연산 주의성
                - 양속성 컨텍스트를 거치지 않고 디비에 바로 삽입하기 때문에 영속성 컨텍스트와 실제 디비의 데이터가 다름
                - select를 했을 경우, 영속성 컨텍스트의 key값과 디비에서 가져온 key값이 동일할 때, 영속성 컨텍스트의 데이터를 최우선으로 하기 때문에
                  디비에서 조회한 값이 bulk update 적용되지 앟는 영속성 컨텍스트의 값이 나오는 문제 발생
        */
        final QMember member = QMember.member;
        final List<Member> beforeMembers = query.selectFrom(member).fetch();
        for (Member findMember : beforeMembers) {
            log.info(" --> Before Member : {}", findMember.toString());
        }

        long count = query.update(member)
                          .set(member.username, "20대")
                          .where(member.age.lt(30))
                          .execute();
        /* 그렇기 때문에 영속성 컨텍스트 초기화를 위한 flush, clear 반드시 필요 */
        em.flush();
        em.clear();
        final List<Member> afterMembers = query.selectFrom(member).fetch();
        for (Member findMember : afterMembers) {
            log.info(" --> After Member : {}", findMember.toString());
        }
    }

    @Test
    @Commit
    @DisplayName("BULK Update")
    void test_dml_01_bulk_update_03() {
        final QMember member = QMember.member;
        long count = query.update(member)
                .set(member.age, member.age.add(100))
                .execute();
        em.flush();
        em.clear();

        final List<Member> afterMembers = query.selectFrom(member).fetch();
        for (Member findMember : afterMembers) {
            log.info(" --> After Member : {}", findMember.toString());
        }
    }

    @Test
    @DisplayName("SQL function 호출하기")
    void test_sqlFunction_01() {
        final QMember member = QMember.member;
        String dobyName =  query.select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                            .from(member).where(member.username.eq("DOBY")).fetchFirst();
        assertEquals("DOBY", dobyName);
    }
}