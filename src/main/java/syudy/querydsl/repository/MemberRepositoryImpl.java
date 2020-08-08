package syudy.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import syudy.querydsl.dto.MemberSearchCondition;
import syudy.querydsl.dto.MemberTeamDto;
import syudy.querydsl.dto.QMemberTeamDto;
import syudy.querydsl.entity.QMember;
import syudy.querydsl.entity.QTeam;

import java.util.List;

import static org.hibernate.annotations.common.util.StringHelper.isEmpty;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    @Autowired
    private JPAQueryFactory query;

    @Override
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

        return query.select(memberTeamDto)
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

    @Override
    public Page<MemberTeamDto> simpleSearch(MemberSearchCondition condition, Pageable pageable) {
        final QTeam team = QTeam.team;
        final QMember member = QMember.member;
        final QMemberTeamDto memberTeamDto = new QMemberTeamDto(
                member.id.as("memberId"),
                member.username.as("userName"),
                member.age,
                team.id.as("teamId"),
                team.name.as("teamName")
        );
        QueryResults<MemberTeamDto> results = query.select(memberTeamDto)
                                                   .from(member)
                                                   .leftJoin(member.team, team)
                                                   .where(
                                                            goeAge(member, condition.getAgeGoe()),
                                                            leoAge(member, condition.getAgeLoe())
                                                    )
                                                   .offset(pageable.getOffset()) // 몇 번째 페이지인지
                                                   .limit(pageable.getPageSize()) // 하나의 페이지에 조회하는 데이터 갯수
                                                   .fetchResults();
        final long total = results.getTotal();
        List<MemberTeamDto> memberTeamDtos = results.getResults();

        return new PageImpl<>(memberTeamDtos, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> complexSearch(MemberSearchCondition condition, Pageable pageable) {
        /* 데이터의 내용과 전체 카운트를 별도로 하는 방법 */
        final QTeam team = QTeam.team;
        final QMember member = QMember.member;
        final QMemberTeamDto memberTeamDto = new QMemberTeamDto(
                member.id.as("memberId"),
                member.username.as("userName"),
                member.age,
                team.id.as("teamId"),
                team.name.as("teamName")
        );
        final List<MemberTeamDto> results = query.select(memberTeamDto)
                                                 .from(member)
                                                 .leftJoin(member.team, team)
                                                 .where(
                                                        goeAge(member, condition.getAgeGoe()),
                                                        leoAge(member, condition.getAgeLoe())
                                                )
                                                 .offset(pageable.getOffset()) // 몇 번째 페이지인지
                                                 .limit(pageable.getPageSize()) // 하나의 페이지에 조회하는 데이터 갯수
                                                 .fetch();
        /*
            contents 를 불러오는 것과 달리, 카운트를 간단하게 할 수 있을 경우,

                - 마지막 페이지일 겨우, offset * size + 조회한 컨텐츠가 전체 데이터라서 구할 필요가 없을 경우
                - 페이지가 0일 경우이고, 컨텐츠 사이즈가 전체 데이터보다 작을 경우 (== 반드시 페이지가 1이상 일 경우)

            그럴때는 별도로 조회하는 방식으로 최적화 가능
        */
        final JPAQuery countQuery = query.select(member)
                                         .setHint("count query", "count query")
                                         .from(member)
                                         .leftJoin(member.team, team)
                                         .where(
                                                goeAge(member, condition.getAgeGoe()),
                                                leoAge(member, condition.getAgeLoe())
                                         );

        return PageableExecutionUtils.getPage(results, pageable, () -> countQuery.fetchCount());
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

    private boolean isNotNull(Integer num) {
        return num != null;
    }

}
