package com.camel.soaptorest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.springws.springsoap.gen.CountriesPort;
import com.springws.springsoap.gen.GetCountryRequest;
import com.springws.springsoap.gen.GetCountryResponse;
import com.springws.springsoap.gen.Country;


import org.springframework.stereotype.Component;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.spi.Tracer;
import org.apache.cxf.message.MessageContentsList;

import org.apache.camel.converter.jaxb.JaxbDataFormat;
import javax.xml.bind.JAXBContext;


@SpringBootApplication()
public  class App {


    /**
     * Says hello to the world.
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Value("${endpoint.wsdl}")
	private String SOAP_URL;

    @Bean(name = "cxfCountryService")
	public CxfEndpoint buildCxfEndpoint() {
		CxfEndpoint cxf = new CxfEndpoint();
		cxf.setAddress(SOAP_URL);
		cxf.setServiceClass(CountriesPort.class);
		return cxf;
	}
    @Component
    class RestApi extends RouteBuilder {

        @Override
        public void configure() {

            JaxbDataFormat jaxb = new JaxbDataFormat();
             try {
                JAXBContext con = JAXBContext.newInstance(GetCountryResponse.class);
            jaxb.setContext(con);
            } catch (Exception e) {
                // TODO: handle exception
                System.out.println(e.getStackTrace());
            }

            restConfiguration()
		.component("undertow").host("0.0.0.0").port(9090).bindingMode(RestBindingMode.auto).scheme("http")
			.dataFormatProperty("prettyPrint", "true")
			.contextPath("/")
				.apiContextPath("/api-doc")
					.apiProperty("api.title", "Camel2Soap")
					.apiProperty("api.version", "1.0")
					.apiProperty("host","")
		.enableCORS(true);

        rest()
			.get("/country/{country}")
				.consumes("text/plain").produces("text/json")
				.description("get country")
				//.param().name("country").type(RestParamType.path).description("country code ").dataType("string").endParam()
                .param().name("country").type(RestParamType.path).description("country code ").endParam()
			.to("direct:soapCountry");

            from("direct:soapCountry")
		        .removeHeaders("CamelHttp*")
		        .process(new Processor() {
			    @Override
			        public void process(Exchange exchange) throws Exception {
                GetCountryRequest request = new GetCountryRequest();
                System.out.println(exchange.getIn().getHeader("country").toString());
                request.setName(exchange.getIn().getHeader("country").toString().toUpperCase());
                exchange.getIn().setBody(request);
			}
		})
		.setHeader(CxfConstants.OPERATION_NAME, constant("{{endpoint.operation.country}}"))
		.setHeader(CxfConstants.OPERATION_NAMESPACE, constant("{{endpoint.namespace}}"))
		.to("cxf:bean:cxfCountryService")
        .log("response country is: ${body[0].country.name}")
		.process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
                System.out.println("camel-message" + exchange.getIn().getBody());
				MessageContentsList response = (MessageContentsList) exchange.getIn().getBody();
                
                GetCountryResponse res = (GetCountryResponse) response.get(0);
                Country country = new Country();
                // if (res == null){
                //     System.out.println("camel-messagenyyyyy" + null);

                //      res = new GetCountryResponse();
                //      country.setName("India");
                //      res.setCountry(country);

                // }
                //System.out.println(res.getCountry().getName());

				exchange.getIn().setBody(res);
			}
		})
        //.unmarshal(jaxb)
		.to("mock:output");

        }
    }
}
