package com.acme.ecommerce.controller;

import com.acme.ecommerce.domain.*;
import com.acme.ecommerce.service.NotEnoughProductAvailableException;
import com.acme.ecommerce.service.ProductNotFoundException;
import com.acme.ecommerce.service.ProductService;
import com.acme.ecommerce.service.PurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

import static com.acme.ecommerce.controller.FlashMessage.Status.FAILURE;
import static com.acme.ecommerce.controller.FlashMessage.Status.SUCCESS;
import static com.acme.ecommerce.util.Utils.calcSubtotal;
import static com.acme.ecommerce.util.Utils.currencyFormatter;

@Controller
@RequestMapping("/cart")
@Scope("request")
public class CartController {
	final Logger logger = LoggerFactory.getLogger(CartController.class);
	
	@Autowired
	PurchaseService purchaseService;
	
	@Autowired
	private ProductService productService;
	
	@Autowired
	private ShoppingCart sCart;
	
	@Autowired
	private HttpSession session;
	
    @RequestMapping("")
    public String viewCart(Model model) {
    	logger.debug("Getting Product List");
    	logger.debug("Session ID = " + session.getId());


		Purchase purchase = sCart.getPurchase();
    	BigDecimal subTotal = new BigDecimal(0);
    	
    	model.addAttribute("purchase", purchase);
    	if (purchase != null) {
    		for (ProductPurchase pp : purchase.getProductPurchases()) {
    			logger.debug("cart has " + pp.getQuantity() + " of " + pp.getProduct().getName());
    			subTotal = subTotal.add(pp.getProduct().getPrice().multiply(new BigDecimal(pp.getQuantity())));
    		}
    	} else {
    		logger.error("No purchases Found for session ID=" + session.getId());
    		return "redirect:/error";
    	}
		model.addAttribute("subTotal", subTotal);
		String s="";
    	if (!sCart.isEmpty()){
    		s=currencyFormatter.apply(subTotal);
		}
		model.addAttribute("subtotalFormatted", s);
        return "cart";
    }
    
    @RequestMapping(path="/add", method = RequestMethod.POST)
    public RedirectView addToCart(@ModelAttribute(value="productId") long productId, @ModelAttribute(value="quantity") int quantity,
								  final RedirectAttributes redirectAttributes) {
    	boolean productAlreadyInCart = false;
    	RedirectView redirect = new RedirectView("/product/");
		redirect.setExposeModelAttributes(false);
    	
    	Product addProduct = productService.findById(productId);
		if (addProduct != null) {
	    	logger.debug("Adding Product: " + addProduct.getId());

    		Purchase purchase = sCart.getPurchase();
    		if (purchase == null) {
    			purchase = new Purchase();
    			sCart.setPurchase(purchase);
    		} else {
    			for (ProductPurchase pp : purchase.getProductPurchases()) {
    				if (pp.getProduct() != null) {
    					if (pp.getProduct().getId().equals(productId)) {
    						int q=pp.getQuantity() + quantity;
							if(productService.isEnoughProductInStock(addProduct,q)) {
								pp.setQuantity(pp.getQuantity() + quantity);
								productAlreadyInCart = true;
								break;
							}else {
								logger.error("Attempt to add more product than available");
								//redirect.setUrl("/error");

							}
    					}
    				}
    			}
    		}
    		if (!productAlreadyInCart) {
    			if(productService.isEnoughProductInStock(addProduct,quantity)) {

					ProductPurchase newProductPurchase = new ProductPurchase();
					newProductPurchase.setProduct(addProduct);
					newProductPurchase.setQuantity(quantity);
					newProductPurchase.setPurchase(purchase);
					purchase.getProductPurchases().add(newProductPurchase);
				}else {
					logger.error("Attempt to add more product than available");
					//redirect.setUrl("/error");
				}
    		}
    		String s=quantity + " " + addProduct.getName() + " added to cart";
    		logger.debug(s);
			redirectAttributes.addFlashAttribute("subtotalFormatted",currencyFormatter.apply(calcSubtotal(purchase)));
    		sCart.setPurchase(purchaseService.save(purchase));
			redirectAttributes.addFlashAttribute("flash",new FlashMessage(s, SUCCESS));
		} else {
			logger.error("Attempt to add unknown product: " + productId);
			redirect.setUrl("/error");
		}

    	return redirect;
    }
 
    @RequestMapping(path="/update", method = RequestMethod.POST)
    public RedirectView updateCart(@ModelAttribute(value="productId") long productId, @ModelAttribute(value="newQuantity") int newQuantity,
								   RedirectAttributes redirectAttributes) {
    	logger.debug("Updating Product: " + productId + " with Quantity: " + newQuantity);
		RedirectView redirect = new RedirectView("/cart");
		redirect.setExposeModelAttributes(false);
    	
    	Product updateProduct = productService.findById(productId);
    	if (updateProduct != null) {
			Purchase purchase = sCart.getPurchase();
			productService.isEnoughProductInStock(updateProduct,newQuantity);

			if (purchase == null) {
    			logger.error("Unable to find shopping cart for update");
    			redirect.setUrl("/error");
    		} else {
				for (ProductPurchase pp : purchase.getProductPurchases()) {
    				if (pp.getProduct() != null) {
    					if (pp.getProduct().getId().equals(productId)) {
    						if (newQuantity > 0) {
    							pp.setQuantity(newQuantity);
    							String s="Updated " + updateProduct.getName() + " to " + newQuantity;
								redirectAttributes.addFlashAttribute("flash",new FlashMessage(s, SUCCESS));
    							logger.debug(s);

    						} else {
    							purchase.getProductPurchases().remove(pp);
								String s="Removed " + updateProduct.getName() + " because quantity was set to " + newQuantity;
								redirectAttributes.addFlashAttribute("flash",new FlashMessage(s, SUCCESS));
    							logger.debug(s);
    						}
    						break;
    					}
    				}
    			}
    		}
    		sCart.setPurchase(purchaseService.save(purchase));

    	} else {
    		logger.error("Attempt to update on non-existent product");
    		redirect.setUrl("/error");
    	}
    	
    	return redirect;
    }
    
    @RequestMapping(path="/remove", method = RequestMethod.POST)
    public RedirectView removeFromCart(@ModelAttribute(value="productId") long productId,final RedirectAttributes redirectAttributes) {
    	logger.debug("Removing Product: " + productId);
		RedirectView redirect = new RedirectView("/cart");
		redirect.setExposeModelAttributes(false);
    	
    	Product updateProduct = productService.findById(productId);
    	if (updateProduct != null) {
    		Purchase purchase = sCart.getPurchase();
    		if (purchase != null) {
    			for (ProductPurchase pp : purchase.getProductPurchases()) {
    				if (pp.getProduct() != null) {
    					if (pp.getProduct().getId().equals(productId)) {
    						purchase.getProductPurchases().remove(pp);
    						String s="Removed " + updateProduct.getName();
							redirectAttributes.addFlashAttribute("flash",new FlashMessage(s, SUCCESS));
   							logger.debug(s);
    						break;
    					}
    				}
    			}
    			purchase = purchaseService.save(purchase);

    			sCart.setPurchase(purchase);
    			if (purchase.getProductPurchases().isEmpty()) {
					redirectAttributes.addFlashAttribute("flash",new FlashMessage("Last item removed from the cart", SUCCESS));
        	    	//if last item in cart redirect to product else return cart
        			redirect.setUrl("/product/");
        		}
    		} else {
    			logger.error("Unable to find shopping cart for update");
    			redirect.setUrl("/error");
    		}
    	} else {
    		logger.error("Attempt to update on non-existent product");
    		redirect.setUrl("/error");
    	}

    	return redirect;
    }
    
    @RequestMapping(path="/empty", method = RequestMethod.POST)
    public RedirectView emptyCart(final RedirectAttributes redirectAttributes) {
    	RedirectView redirect = new RedirectView("/product/");
		redirect.setExposeModelAttributes(false);
    	
    	logger.debug("Emptying Cart");
    	Purchase purchase = sCart.getPurchase();
		if (purchase != null) {
			purchase.getProductPurchases().clear();
			sCart.setPurchase(purchaseService.save(purchase));
			redirectAttributes.addFlashAttribute("flash",new FlashMessage("The cart was emptied", SUCCESS));
		} else {
			logger.error("Unable to find shopping cart for update");
			redirect.setUrl("/error");
		}
		
    	return redirect;
    }


	@ExceptionHandler(NotEnoughProductAvailableException.class)
	public String productError(Model model, HttpServletRequest req, Exception ex) {
		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(req);
		if(flashMap != null) {
			flashMap.put("flash",new FlashMessage(ex.getMessage(), FAILURE));
		}
		return "redirect:" +req.getHeader("referer");
	}

}
