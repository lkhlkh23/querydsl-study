package syudy.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import syudy.querydsl.dto.MemberSearchCondition;
import syudy.querydsl.dto.MemberTeamDto;
import syudy.querydsl.dto.QMemberTeamDto;
import syudy.querydsl.entity.Member;
import syudy.querydsl.entity.QMember;
import syudy.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.hibernate.annotations.common.util.StringHelper.isEmpty;
import static org.hibernate.annotations.common.util.StringHelper.isNotEmpty;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    /* EntityManager 트랜잭션 단위로 분리해서 동작하기 때문에 동시성 문제 X (프록시 사용) JPA책 13-1 참고 */
    private final EntityManager em;
    private final JPAQueryFactory query;

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        final Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                 .getResultList();
    }

    public List<Member> findAllUsingQuerydsl() {
        return query.selectFrom(QMember.member).fetch();
    }

    public Member findByUserName(String name) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                 .setParameter("username", name)
                 .getSingleResult();
    }

    public Member findByUserNameUsingQuerydsl(String name) {
        final QMember member = QMember.member;
        return query.selectFrom(member).where(member.username.eq(name)).fetchOne();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        final QTeam team = QTeam.team;
        final QMember member = QMember.member;
        final QMemberTeamDto memberTeamDto = new QMemberTeamDto(
                member.id.as("memberId"),
                member.username.as("userName"),
                member.age,
                team.id.as("teamId"),
                team.name.as("teamName")
        );

        final BooleanBuilder builder = new BooleanBuilder();
        builder.and(eqUserName(member, condition.getUserName()));
        builder.and(eqTeamName(team, condition.getTeamName()));
        builder.and(goeAge(member, condition.getAgeGoe()));
        builder.and(leoAge(member, condition.getAgeLoe()));

        return query.select(
                        new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username.as("userName"),
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")
                        )
                    )
                    .from(member)
                    .leftJoin(member.team, team)
                    .where(
                        eqTeamName(team, condition.getTeamName()),
                        eqUserName(member, condition.getUserName()),
                        goeAge(member, condition.getAgeGoe()),
                        leoAge(member, condition.getAgeLoe())
                    )
                    .fetch();
    }

    private BooleanExpression eqTeamName(QTeam team, String teamName) {
        return isEmpty(teamName) ? null : team.name.eq(teamName);
    }

    private BooleanExpression leoAge(QMember member, Integer age) {
        return isNotNull(age) ? member.age.loe(age) : null;
    }

    private BooleanExpression goeAge(QMember member, Integer age) {
        return isNotNull(age) ? member.age.goe(age) : null;
    }

    private BooleanExpression eqUserName(QMember member, String userName) {
        return isEmpty(userName) ? null : member.username.eq(userName);
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        final QMember member = QMember.member;
        final QTeam team = QTeam.team;
        final QMemberTeamDto memberTeamDto = new QMemberTeamDto(
                member.id.as("memberId"),
                member.username.as("userName"),
                member.age,
                team.id.as("teamId"),
                team.name.as("teamName")
        );

        final BooleanBuilder builder = new BooleanBuilder();
        if(isNotEmpty(condition.getUserName())) {
            builder.and(member.username.eq(condition.getUserName()));
        }

        if(isNotEmpty(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if(isNotNull(condition.getAgeGoe())) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if(isNotNull(condition.getAgeLoe())) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return query.select(memberTeamDto)
                    .from(member)
                    .leftJoin(member.team, team)
                    .where(builder)
                    .fetch();
    }

    private boolean isNotNull(Integer num) {
        return num != null;
    }

}
