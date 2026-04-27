package org.lml.AiGateWay.api;

import org.lml.common.dto.UserLoginReq;
import org.lml.common.result.CommonResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


public interface UserFacadeService {

    CommonResult doLogin(@RequestBody @Validated UserLoginReq userLoginReq);

    CommonResult<String> logout(@RequestParam("userId") String userId);
}
