function [ individualP, matchingP ] = matchingProbAssetCash_AC( N )
%MATCHINGPROBASSETCASH Summary of this function goes here
%   Detailed explanation goes here

    agents = zeros( N, 1 );
    individualP = zeros( N, 2 );
    matchingP = zeros( N, 1 );
    xLabels = [];
    
    for i = 0 : N
        agents( i + 1 ) = i / N;
        
        a = { sprintf( '%d (%0.2f)', i, agents( i + 1 ) ) };
        xLabels{ i + 1 } = a;
    end
    
    pU = 1.0;
    pD = 0.2;
  
    for i = 0 : N - 1
        indexA = i + 1;
        indexB = i + 2;
        
        ha = agents( indexA );
        hb = agents( indexB );
        
        limitPriceAsk = ha * pU + ( 1.0 - ha ) * pD;
        limitPriceBid = hb * pU + ( 1.0 - hb ) * pD;
        
        limitPriceRange = limitPriceBid - limitPriceAsk;
        
        % probability for asker to fall into the potential matching range
        pAsk = limitPriceRange / ( pU - limitPriceAsk );
        % probability for the bider to fall into the potential matching
        % range
        pBid = limitPriceRange / ( limitPriceBid - pD );
        
        % store probabilities
        individualP( i + 1, 1 ) = pAsk;
        individualP( i + 1, 2 ) = pBid;
        
        % calculate probabilities that asker AND bider fall both into
        % matching range: bayes theorem, just multiply
        matchingP( i + 1 ) = pAsk * pBid * 0.5;
    end

   	fontSizeValue = 14;
    
    plot( individualP, '-X' );
    %title( 'Askers and biders individual probability falling into matching range on Asset/Cash market' );
    l = legend( 'Askers', 'Biders' );
    set(l,'FontSize', fontSizeValue);
    l = xlabel( 'Agent optimism' );
    set(l,'FontSize', fontSizeValue);
    l = ylabel( 'Probability' );
    set(l,'FontSize', fontSizeValue);
    set(gca, 'XTick', 1:length(xLabels)); % Change x-axis ticks
    set(gca,'FontSize', 12);
    ticLoc = get( gca, 'XTick' );
    ticLab = cellfun(@(x) char( xLabels{ x } ),num2cell(ticLoc),'UniformOutput',false);
    set(gca,'XTickLabel', ceil(agents*1000)/1000); % Change x-axis ticks labels to desired values.

    figure
    
    plot( matchingP, '-X' );
    %title( 'Askers and biders probability to match on Asset/Cash market' );
    l = legend( 'Matching-Probability' );
    set(l,'FontSize', fontSizeValue);
    l = xlabel( 'Agent optimism' );
    set(l,'FontSize', fontSizeValue);
    l = ylabel( 'Probability' );
    set(l,'FontSize', fontSizeValue);
    set(gca,'FontSize', 12);
    set(gca, 'XTick', 1:length(agents)); % Change x-axis ticks
    set( gca,'XTickLabel', ceil(agents*1000)/1000); % Change x-axis ticks labels to desired values.

end

