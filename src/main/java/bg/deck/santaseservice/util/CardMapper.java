package bg.deck.santaseservice.util;

import bg.deck.santaseservice.model.Card;
import bg.deck.santaseservice.model.dto.CardDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {EnumMapper.class})
public interface CardMapper {

    @Mapping(source = "suit", target = "suit")
    @Mapping(source = "rank", target = "rank")
    CardDTO toDTO(Card card);

    List<CardDTO> toDTO(List<Card> cards);
}
