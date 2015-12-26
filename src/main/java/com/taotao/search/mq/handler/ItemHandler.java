package com.taotao.search.mq.handler;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taotao.common.service.ApiService;
import com.taotao.search.bean.Item;

@Component
public class ItemHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private HttpSolrServer httpSolrServer;

    @Autowired
    private ApiService apiService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemHandler.class);

    /**
     * 处理商品消息
     * 
     * @param msg
     * @throws Exception
     * @throws
     */
    public void handler(String msg) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("接收到的消息为：{}", msg);
        }
        try {
            JsonNode jsonNode = MAPPER.readTree(msg);
            String type = jsonNode.get("type").asText();
            Long id = jsonNode.get("id").asLong();
            if (StringUtils.equals(type, "update") || StringUtils.equals(type, "insert")) {
                // 从后台管理系统中查询最新的商品数据
                String url = "http://manage.taotao.com/rest/item/" + id;
                String jsonData = this.apiService.doGet(url);
                Item item = MAPPER.readValue(jsonData, Item.class);
                this.httpSolrServer.addBean(item);//新增和更新
                this.httpSolrServer.commit();
            } else if (StringUtils.equals(type, "delete")) {
                this.httpSolrServer.deleteById(String.valueOf(id));//删除
                this.httpSolrServer.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
