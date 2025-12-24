package bg.deck.santaseservice.util;

import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.request.RegisterRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(RegisterRequest registerRequest);
}
