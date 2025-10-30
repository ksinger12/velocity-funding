package com.funding.velocity.controller;

import com.funding.velocity.service.LoadFundingService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoadFundingController {

  public final LoadFundingService loadFundingService;

  public LoadFundingController(LoadFundingService loadFundingService) {
    this.loadFundingService = loadFundingService;
  }

  @PostMapping("/load-fund-data")
  public String loadFunds(@RequestBody JSONObject json) {

    // json schema validatino

    return "";
  }

}
