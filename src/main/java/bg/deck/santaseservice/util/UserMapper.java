package bg.deck.santaseservice.util;

import bg.deck.santaseservice.constant.RankingConstants;
import bg.deck.santaseservice.enums.Rank;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.request.RegisterRequest;
import bg.deck.santaseservice.model.response.ProfileResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import static bg.deck.santaseservice.enums.Role.ROLE_USER;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(RegisterRequest registerRequest);

    @AfterMapping
    default void setDefaults(@MappingTarget User user) {
        user.setRole(ROLE_USER);
        user.setSantaseWins(0);
        user.setSantaseLosses(0);
        user.setIsEmailConfirmed(false);
        user.setRank(Rank.UNRANKED);
        user.setRankRating(RankingConstants.INITIAL_THRESHOLD);
    }

    @Mapping(source = "rank", target = "rank")
    @Mapping(source = "isEmailConfirmed", target = "emailConfirmed")
    ProfileResponse toProfileResponse(User user);
}
