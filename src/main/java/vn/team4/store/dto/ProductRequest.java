package vn.team4.store.dto;

public class ProductRequest {
    private String id;
    private String name;
    private double import_price;
    private double selling_price;
    private double quantity;
    private String category;

    public ProductRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getImport_price() {
        return import_price;
    }
    
    public void setImport_price(double import_price) {
        this.import_price = import_price;
    }

    public double getSelling_price() {
        return selling_price;
    }

    public void setSelling_price(double selling_price) {
        this.selling_price = selling_price;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
