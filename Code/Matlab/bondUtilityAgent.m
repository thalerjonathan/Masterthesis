function [ utility ] = bondUtilityAgent( h, j )
%BONDUTILITYAGENT Summary of this function goes here
%   Detailed explanation goes here


    % value of tomorrows UP
    pU = 1.0;
    % value of tomorrows DOWN
    pD = 0.2;
    limitPriceAsset = h * pU + ( 1 - h ) * pD;
    limitPriceBond = h * j + ( 1 - h ) * pD;

    STEPS = 50;
    utility = [];

    xLabels = [];
    yLabels = [];
    
    for x = 1 : STEPS
        p = pD + ( x / STEPS ) * ( pU - pD );
        xLabels( x ) = p;

        for y = 1 : STEPS
            q = ( y / STEPS ) * j;
            yLabels( y ) = q;
            
            utility( x, y ) = p - limitPriceAsset + (p/q) * ( limitPriceBond - q );
        end
    end

    surf(utility);
    xlabel( 'Asset-Price' );
    ylabel( 'Bond-Price' );
    zlabel( 'Utility' );
    
    set(gca,'XTick', 1:length(xLabels)); % Change x-axis ticks
    set(gca,'XTickLabel', xLabels); % Change x-axis ticks labels to desired values.
    set(gca,'YTick', 1:length(yLabels)); % Change x-axis ticks
    set(gca,'YTickLabel', yLabels); % Change x-axis ticks labels to desired values.

end

