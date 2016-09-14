package com.wso2telco.core.datamigrator.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisUtil {

	// address of your redis server
	private static final String redisHost = "localhost";
	private static final Integer redisPort = 6379;
	private static final String redisPass = "cY4L3dBf@mifenew";

	// the jedis connection pool..
	private static JedisPool pool = null;

	private static Jedis jedis = null;
	
	public static Jedis getJedis() {
		// configure our pool connection
		pool = new JedisPool(redisHost, redisPort);
		jedis = pool.getResource();
		jedis.auth(redisPass);
		return jedis;
	}
}
