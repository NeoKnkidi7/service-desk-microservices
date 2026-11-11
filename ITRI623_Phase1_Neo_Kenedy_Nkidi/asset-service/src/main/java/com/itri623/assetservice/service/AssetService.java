package com.itri623.assetservice.service;

import com.itri623.assetservice.model.Asset;
import com.itri623.assetservice.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    public Optional<Asset> getAssetById(Long id) {
        return assetRepository.findById(id);
    }

    public Asset createAsset(Asset asset) {
        return assetRepository.save(asset);
    }

    public Asset updateAsset(Long id, Asset updatedAsset) {

        Asset existingAsset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Asset with ID " + id + " not found"
                ));

        existingAsset.setName(updatedAsset.getName());
        existingAsset.setSerialNumber(updatedAsset.getSerialNumber());
        existingAsset.setLocation(updatedAsset.getLocation());

        return assetRepository.save(existingAsset);
    }

    public void deleteAsset(Long id) {
        assetRepository.deleteById(id);
    }
}