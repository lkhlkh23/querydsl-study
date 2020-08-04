package syudy.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection
    public MemberDto(String username) {
        this.username = username;
    }

    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}