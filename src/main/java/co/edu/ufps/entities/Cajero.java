package co.edu.ufps.entities;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "cajero")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cajero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 20)
    private String documento;

    @ManyToOne
    @JoinColumn(name = "tienda_id", nullable = false)
    @JsonIgnore
    private Tienda tienda;

    @Column(length = 50)
    private String email;

    @Column(length = 100)
    private String token;
}