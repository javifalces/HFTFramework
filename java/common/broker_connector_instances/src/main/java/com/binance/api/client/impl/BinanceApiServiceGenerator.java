package com.binance.api.client.impl;

import com.binance.api.client.BinanceApiError;
import com.binance.api.client.config.BinanceApiConfig;
import com.binance.api.client.exception.BinanceApiException;
import com.binance.api.client.security.AuthenticationInterceptor;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

/**
 * Generates a Binance API implementation based on @see {@link BinanceApiService}.
 */
public class BinanceApiServiceGenerator {

	private static final OkHttpClient sharedClient;
	private static final Converter.Factory converterFactory = JacksonConverterFactory.create();

	static {
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequestsPerHost(500);
		dispatcher.setMaxRequests(500);
		sharedClient = new OkHttpClient.Builder().dispatcher(dispatcher).pingInterval(20, TimeUnit.SECONDS).build();
	}

	@SuppressWarnings("unchecked") private static final Converter<ResponseBody, BinanceApiError> errorBodyConverter = (Converter<ResponseBody, BinanceApiError>) converterFactory
			.responseBodyConverter(BinanceApiError.class, new Annotation[0], null);

	public static <S> S createService(Class<S> serviceClass) {
		return createService(serviceClass, null, null);
	}

	public static <S> S createService(Class<S> serviceClass, String apiKey, String secret) {
		Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(BinanceApiConfig.getApiBaseUrl())
				.addConverterFactory(converterFactory);

		if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(secret)) {
			retrofitBuilder.client(sharedClient);
		} else {
			// `adaptedClient` will use its own interceptor, but share thread pool etc with the 'parent' client
			AuthenticationInterceptor interceptor = new AuthenticationInterceptor(apiKey, secret);
			OkHttpClient adaptedClient = sharedClient.newBuilder().addInterceptor(interceptor).build();
			retrofitBuilder.client(adaptedClient);
		}

		Retrofit retrofit = retrofitBuilder.build();
		return retrofit.create(serviceClass);
	}

	/**
	 * Execute a REST call and block until the response is received.
	 */
	public static <T> T executeSync(Call<T> call) {
		try {
			Response<T> response = call.execute();
			if (response.isSuccessful()) {
				return response.body();
			} else {
				BinanceApiError apiError = getBinanceApiError(response);
				throw new BinanceApiException(apiError);
			}
		} catch (IOException e) {
			throw new BinanceApiException(e);
		}
	}

	/**
	 * Extracts and converts the response error body into an object.
	 */
	public static BinanceApiError getBinanceApiError(Response<?> response) throws IOException, BinanceApiException {
		return errorBodyConverter.convert(response.errorBody());
	}

	/**
	 * Returns the shared OkHttpClient instance.
	 */
	public static OkHttpClient getSharedClient() {
		return sharedClient;
	}
}
