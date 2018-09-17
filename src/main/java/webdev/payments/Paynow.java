package webdev.payments;

import webdev.core.*;
import webdev.exceptions.*;
import webdev.helpers.*;
import webdev.http.*;
import java.util.*;
import java.math.*;


public class Paynow
{
	/** 
		 Paynow constructor
	 
	 @param integrationId
	 @param integrationKey
	 @param resultUrl
	 @exception ArgumentException
	*/

	public Paynow(String integrationId, String integrationKey)
	{
		this(integrationId, integrationKey, null);
	}

	public Paynow(String integrationId, String integrationKey, String resultUrl)
	{
		if (tangible.StringHelper.isNullOrEmpty(integrationId))
		{
			throw new IllegalArgumentException("Integration id cannot be empty", "integrationId");
		}
		if (tangible.StringHelper.isNullOrEmpty(integrationKey))
		{
			throw new IllegalArgumentException("Integration key cannot be empty", "integrationKey");
		}


		setIntegrationId(integrationId);
		setIntegrationKey(UUID.fromString(integrationKey));

		if (resultUrl != null)
		{
			setResultUrl(resultUrl);
		}


		setClient(new Client());
	}

	/** 
		 Merchant's return url
	*/
	private String ResultUrl = "http://localhost";
	public final String getResultUrl()
	{
		return ResultUrl;
	}
	public final void setResultUrl(String value)
	{
		ResultUrl = value;
	}

	/** 
		 Merchant's result url
	*/
	private String ReturnUrl = "http://localhost";
	public final String getReturnUrl()
	{
		return ReturnUrl;
	}
	public final void setReturnUrl(String value)
	{
		ReturnUrl = value;
	}

	/** 
		 Merchant's integration id
	*/
	private UUID IntegrationKey;
	public final UUID getIntegrationKey()
	{
		return IntegrationKey;
	}
	public final void setIntegrationKey(UUID value)
	{
		IntegrationKey = value;
	}

	/** 
		 Client for making http requests
	*/
	private Client Client;
	public final Client getClient()
	{
		return Client;
	}
	public final void setClient(Client value)
	{
		Client = value;
	}

	/** 
		 Merchant's integration key
	*/
	private String IntegrationId;
	public final String getIntegrationId()
	{
		return IntegrationId;
	}
	public final void setIntegrationId(String value)
	{
		IntegrationId = value;
	}

	/** 
		 Creates a new transaction
	 
	 @param reference
	 @param values
	 @param authEmail
	 @return 
	*/

	public final Payment CreatePayment(String reference, java.util.HashMap<String, java.math.BigDecimal> values)
	{
		return CreatePayment(reference, values, "");
	}

	public final Payment CreatePayment(String reference)
	{
		return CreatePayment(reference, null, "");
	}

	public final Payment CreatePayment(String reference, HashMap<String, BigDecimal> values, String authEmail)
	{
		return values != null ? new Payment(reference, values, authEmail) : new Payment(reference, authEmail);
	}

	/** 
		 Sends a payment to paynow
	 
	 @param payment
	 @return 
	 @exception InvalidReferenceException
	 @exception EmptyCartException
	*/
	public final InitResponse Send(Payment payment)
	{
		if (tangible.StringHelper.isNullOrEmpty(payment.getReference()))
		{
			throw new InvalidReferenceException();
		}

		if (payment.getTotal().compareTo(0) <= 0)
		{
			throw new EmptyCartException();
		}

		return Init(payment);
	}

	public final StatusResponse PollTransaction(String url)
	{
		String response = getClient().PostAsync(url, null);
		HashMap<Object, Object> data = HttpUtility.ParseQueryString(response).ToDictionary();

		if (!data.containsKey("hash") || Hash.Verify(data, getIntegrationKey()))
		{
			throw new HashMismatchException();
		}

		return new StatusResponse(data);
	}

	/** 
		 Process a status update from Paynow
	 
	 @param response Raw POST string sent from Paynow
	 @return 
	 @exception HashMismatchException
	*/
	public final StatusResponse ProcessStatusUpdate(String response)
	{
		HashMap<Object, Object> data = HttpUtility.ParseQueryString(response).ToDictionary();

		if (!data.containsKey("hash") || Hash.Verify(data, getIntegrationKey()))
		{
			throw new HashMismatchException();
		}

		return new StatusResponse(data);
	}


	/** 
		 Process a status update from Paynow
	 
	 @param response Key-value pairs of data sent from Paynow
	 @return 
	 @exception HashMismatchException
	*/
	public final StatusResponse ProcessStatusUpdate(HashMap<String, String> response)
	{
		if (!response.containsKey("hash") || Hash.Verify(response, getIntegrationKey()))
		{
			throw new HashMismatchException();
		}

		return new StatusResponse(response);
	}

	/** 
		 Send a mobile transaction to paynow
	 
	 @param payment
	 @param phone
	 @param method
	 @return 
	 @exception InvalidReferenceException
	 @exception EmptyCartException
	 @exception ArgumentException
	*/

	public final InitResponse SendMobile(Payment payment, String phone)
	{
		return SendMobile(payment, phone, MobileMoneyMethod.Ecocash);
	}
	
	public final InitResponse SendMobile(Payment payment, String phone, MobileMoneyMethod method)
	{
		if (tangible.StringHelper.isNullOrEmpty(payment.getReference()))
		{
			throw new InvalidReferenceException();
		}

		if (payment.getTotal().compareTo(0) <= 0)
		{
			throw new EmptyCartException();
		}

		if (!Regex.IsMatch(phone, "07([7,8])((\\1=7)[1-9]|[2-5])\\d{6}"))
		{
			throw new IllegalArgumentException("Invalid phone number", "phone");
		}

		return InitMobile(payment, phone, method);
	}

	/** 
		 Initiate a new Paynow mobile transaction
	 
	 @param payment
	 @param phone
	 @param method
	 @return 
	*/
	private InitResponse InitMobile(Payment payment, String phone, MobileMoneyMethod method)
	{
		HashMap<String, String> data = FormatMobileInitRequest(payment, phone, method);

		String response = getClient().PostAsync(Constants.UrlInitiateTransaction, data);

		if (!data.containsKey("hash") || Hash.Verify(data, getIntegrationKey()))
		{
			throw new HashMismatchException();
		}

		return new InitResponse(HttpUtility.ParseQueryString(response).ToDictionary());
	}


	/** 
		 Initiate a new Paynow transaction
	 
	 @param payment
	 @return 
	 @exception NotImplementedException
	*/
	private InitResponse Init(Payment payment)
	{
		HashMap<String, String> data = FormatInitRequest(payment);

		String response = getClient().PostAsync(Constants.UrlInitiateTransaction, data);

		if (!data.containsKey("hash") || !Hash.Verify(data, getIntegrationKey()))
		{
			throw new HashMismatchException();
		}

		return new InitResponse(HttpUtility.ParseQueryString(response).ToDictionary());
	}

	/** 
		 Formats an init request before its sent to Paynow
	 
	 @param payment
	 @return 
	*/
	private HashMap<String, String> FormatInitRequest(Payment payment)
	{
		HashMap<Object, Object> items = payment.ToDictionary();

		items.put("returnurl", getReturnUrl().trim());
		items.put("resulturl", getResultUrl().trim());
		items.put("id", getIntegrationId());

		items.put("hash", Hash.Make(items, getIntegrationKey()));

		return items;
	}

	/** 
		 Initiate a new Paynow transaction
	 
	 
		 Currently, only eccocash is supported
	 
	 @param payment The transaction to be sent to Paynow
	 @param phone The user's phone number
	 @param method The mobile transaction method i.e ecocash, telecash
	 @return 
	*/
	private HashMap<String, String> FormatMobileInitRequest(Payment payment, String phone, MobileMoneyMethod method)
	{
		HashMap<Object, Object> items = payment.ToDictionary();

		items.put("returnurl", getReturnUrl().trim());
		items.put("resulturl", getResultUrl().trim());
		items.put("id", getIntegrationId());
		items.put("phone", phone);
		items.put("method", Webdev.Helpers.Extensions.GetString(method));

		items.put("hash", Hash.Make(items, getIntegrationKey()));

		return items;
	}
}