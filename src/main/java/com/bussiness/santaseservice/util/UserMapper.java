package com.bussiness.santaseservice.util;

import com.bussiness.santaseservice.model.User;
import com.bussiness.santaseservice.model.request.RegisterRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(RegisterRequest registerRequest);
}
